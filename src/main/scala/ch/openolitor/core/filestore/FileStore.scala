/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor.core.filestore

import akka.actor.ActorSystem
import ch.openolitor.core.models.BaseStringId
import ch.openolitor.core.{ JSONSerializable, MandantConfiguration }
import com.amazonaws.auth.{ AWSStaticCredentialsProvider, BasicAWSCredentials }
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3ClientBuilder, S3ClientOptions }
import com.amazonaws.{ AmazonClientException, ClientConfiguration }
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime

import java.io.InputStream
import java.util.UUID
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

case class FileStoreError(message: String)
case class FileStoreSuccess()

case class FileStoreFileMetadata(name: String, fileType: FileType)
case class FileStoreFile(metaData: FileStoreFileMetadata, file: InputStream)
case class FileStoreFileId(id: String) extends BaseStringId with Ordered[FileStoreFileId] {
  def compare(that: FileStoreFileId): Int = (this.id) compare (that.id)
}
case class FileStoreFileReference(fileType: FileType, id: FileStoreFileId) extends JSONSerializable
case class FileStoreChunkedUploadMetaData(key: String, uploadId: String, bucket: FileStoreBucket, metadata: FileStoreFileMetadata)
case class FileStoreChunkedUploadPartEtag(partNumber: Int, etag: String)
case class FileStoreFileSummary(key: String, etag: String, size: Long, lastModified: DateTime)

trait FileStore {
  val mandant: String

  /**
   * Get a list of file summaries of a given bucket.
   *
   * @param bucket the bucket to get the list of summaries from
   * @return either a list of `FileStoreFileSummary`s of the given bucket or `FileStoreError`
   */
  def getFileSummaries(bucket: FileStoreBucket): Future[Either[FileStoreError, List[FileStoreFileSummary]]]

  /**
   * List the files of a given bucket.
   *
   * @param bucket the bucket to list the files.
   * @return either a list of `FileStoreFileId`s of the given bucket or a `FileStoreError`.
   */
  def getFileIds(bucket: FileStoreBucket): Future[Either[FileStoreError, List[FileStoreFileId]]]

  /**
   * Get the file by id.
   *
   * @param bucket the bucket where the file with the id is stored.
   * @param id the id of the file.
   * @return either a `FileStoreFile` or a `FileStoreError`.
   */
  def getFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreFile]]

  /**
   * Upload the file to the bucket with the id.
   *
   * @param bucket the bucket where the file should be stored.
   * @param id the id of the file. If left None an id will be generated.
   * @param metadata the required `FileStoreFileMetadata` to store with this file.
   * @param file the file as `InputStream`.
   * @return either the resulting `FileStoreFileMetadata` or a `FileStoreError`.
   */
  def putFile(bucket: FileStoreBucket, id: Option[String], metadata: FileStoreFileMetadata, file: InputStream): Future[Either[FileStoreError, FileStoreFileMetadata]]

  /**
   * Delete the file in the given bucket by id.
   *
   * @param bucket the bucket where the file should be deleted.
   * @param id the id of the file to delete.
   * @return either `FileStoreSuccess` or `FileStoreError`.
   */
  def deleteFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreSuccess]]

  /**
   * Delete the files in the given bucket having the given ids.
   *
   * @param bucket the bucket where the file should be deleted.
   * @param ids the ids of the files to delete.
   * @return either `FileStoreSuccess` or `FileStoreError`.
   */
  def deleteFiles(bucket: FileStoreBucket, ids: List[String]): Future[Either[FileStoreError, FileStoreSuccess]]

  /**
   * Create the buckets listed in `FileStoreBucket.AllFileStoreBuckets` if they do not exist already.
   *
   * @return either `FileStoreSuccess` or `FileStoreError`.
   */
  def createBuckets: Future[Either[FileStoreError, FileStoreSuccess]]

  /**
   * Initiate the chunked upload of data which will be concatenated in the end after calling `completeChunkedUpload`.
   *
   * @param bucket the bucket where the file should be stored.
   * @param id the id of the file. If left None an id will be generated.
   * @param metadata the required metadata for this upload.
   * @return the resulting `FileStoreChunkedUploadMetaData` or `FileStoreError`.
   */
  def initiateChunkedUpload(bucket: FileStoreBucket, id: Option[String], metadata: FileStoreFileMetadata): Future[Either[FileStoreError, FileStoreChunkedUploadMetaData]]

  /**
   * Upload a part to the already initiated chunked upload process. Initiate the chunked upload first using `initiateChunkedUpload`.
   *
   * @param metadata the metadata returned by `initiateChunkedUpload`.
   * @param part the part as `InputStream`.
   * @param partSize the size of this part.
   * @param partNumber the number of this part.
   * @return the resulting `FileStoreChunkedUploadPartEtag` for this part or `FileStoreError`.
   */
  def uploadChunk(metadata: FileStoreChunkedUploadMetaData, part: InputStream, partSize: Int, partNumber: Int): Future[Either[FileStoreError, FileStoreChunkedUploadPartEtag]]

  /**
   * Complete the chunked upload process identified by the given metadata.
   *
   * @param metadata the metadata returned by `initiateChunkedUpload`.
   * @param partEtags the etags sorted by partNumber.
   * @return either `FileStoreChunkedUploadMetaData` or `FileStoreError`.
   */
  def completeChunkedUpload(metadata: FileStoreChunkedUploadMetaData, partEtags: List[FileStoreChunkedUploadPartEtag]): Future[Either[FileStoreError, FileStoreChunkedUploadMetaData]]

  /**
   * Abort the chunked upload process identified by the given metadata.
   *
   * @param metadata
   * @return either `FileStoreChunkedUploadMetaData` or `FileStoreError`.
   */
  def abortChunkedUpload(metadata: FileStoreChunkedUploadMetaData): Future[Either[FileStoreError, FileStoreChunkedUploadMetaData]]

  /**
   * Retrieve the string representation of the given bucket.
   *
   * @param bucket the bucket.
   * @return the bucket name as `String` of the given `FileStoreBucket`
   */
  def bucketName(bucket: FileStoreBucket): String = {
    s"${mandant.toLowerCase}-${bucket.toString.toLowerCase}"
  }
}

class S3FileStore(override val mandant: String, override val client: AmazonS3) extends FileStore with FileStoreBucketLifeCycleConfiguration with LazyLogging {
  def getFileSummaries(bucket: FileStoreBucket): Future[Either[FileStoreError, List[FileStoreFileSummary]]] = {
    Future.successful {
      try {
        Right(listObjects(bucket))
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError("Could not get file summaries."))
      }
    }
  }

  def getFileIds(bucket: FileStoreBucket): Future[Either[FileStoreError, List[FileStoreFileId]]] = {
    Future.successful {
      try {
        Right(listObjects(bucket).map(s => FileStoreFileId(s.key)))
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError("Could not get file ids."))
      }
    }
  }

  def getFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreFile]] = {
    Future.successful {
      try {
        val result = client.getObject(new GetObjectRequest(bucketName(bucket), id))
        Right(FileStoreFile(transform(result.getObjectMetadata.getUserMetadata.asScala.toMap), result.getObjectContent))
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not get file. ${e}"))
      }
    }
  }

  def putFile(bucket: FileStoreBucket, id: Option[String], metadata: FileStoreFileMetadata, file: InputStream): Future[Either[FileStoreError, FileStoreFileMetadata]] = {
    Future.successful {
      try {
        client.putObject(new PutObjectRequest(bucketName(bucket), id.getOrElse(generateId), file, transform(metadata)))
        Right(metadata)
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not put file. ${e}"))
      }
    }
  }

  def deleteFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreSuccess]] = {
    Future.successful {
      try {
        client.deleteObject(new DeleteObjectRequest(bucketName(bucket), id))
        Right(FileStoreSuccess())
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not delete file with key $id."))
      }
    }
  }

  def deleteFiles(bucket: FileStoreBucket, ids: List[String]): Future[Either[FileStoreError, FileStoreSuccess]] = {
    Future.successful {
      try {
        client.deleteObjects(new DeleteObjectsRequest(bucketName(bucket)).withKeys(ids: _*))
        Right(FileStoreSuccess())
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not delete files with keys ${ids.mkString}."))
      }
    }
  }

  def initiateChunkedUpload(bucket: FileStoreBucket, id: Option[String], metadata: FileStoreFileMetadata): Future[Either[FileStoreError, FileStoreChunkedUploadMetaData]] = {
    Future.successful {
      try {
        val key = id.getOrElse(generateId)
        val initiateResult = client.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName(bucket), key, transform(metadata)))

        Right(FileStoreChunkedUploadMetaData(key, initiateResult.getUploadId, bucket, metadata))
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not initiate chunked upload. ${e}"))
      }
    }
  }

  def uploadChunk(metadata: FileStoreChunkedUploadMetaData, part: InputStream, partSize: Int, partNumber: Int): Future[Either[FileStoreError, FileStoreChunkedUploadPartEtag]] = {
    Future.successful {
      try {
        val uploadPart = new UploadPartRequest()
          .withKey(metadata.key)
          .withPartNumber(partNumber)
          .withBucketName(bucketName(metadata.bucket))
          .withInputStream(part)
          .withUploadId(metadata.uploadId)
          .withPartSize(partSize)

        val singleResult = client.uploadPart(uploadPart)

        Right(FileStoreChunkedUploadPartEtag(singleResult.getPartETag.getPartNumber, singleResult.getPartETag.getETag))
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not upload chunked part of file. ${e}"))
      }
    }
  }

  def completeChunkedUpload(metadata: FileStoreChunkedUploadMetaData, etags: List[FileStoreChunkedUploadPartEtag]): Future[Either[FileStoreError, FileStoreChunkedUploadMetaData]] = {
    Future.successful {
      try {
        val partEtags = etags map (p => new PartETag(p.partNumber, p.etag))

        client.completeMultipartUpload(new CompleteMultipartUploadRequest(
          bucketName(metadata.bucket),
          metadata.key,
          metadata.uploadId,
          partEtags.asJava
        ))

        Right(metadata)
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not complete chunked file upload. ${e}"))
      }
    }
  }

  def abortChunkedUpload(metadata: FileStoreChunkedUploadMetaData): Future[Either[FileStoreError, FileStoreChunkedUploadMetaData]] = {
    Future.successful {
      try {
        client.abortMultipartUpload(new AbortMultipartUploadRequest(
          bucketName(metadata.bucket), metadata.key, metadata.uploadId
        ))

        Right(metadata)
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not abort chunked file upload. ${e}"))
      }
    }
  }

  override def createBuckets: Future[Either[FileStoreError, FileStoreSuccess]] = {
    Future.successful {
      try {
        val result = FileStoreBucket.AllFileStoreBuckets map { bucket =>
          client.createBucket(new CreateBucketRequest(bucketName(bucket)))

          configureLifeCycle(bucket)
        }
        Right(FileStoreSuccess())
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not create buckets. $e"))
      }
    }
  }

  protected def generateId = UUID.randomUUID.toString

  protected def transform(metadata: Map[String, String]): FileStoreFileMetadata = {
    if (metadata.isEmpty) logger.warn("There was no metadata stored with this file.")
    val fileType = FileType(metadata.get("fileType").getOrElse("").toLowerCase)
    FileStoreFileMetadata(metadata.get("name").getOrElse(""), fileType)
  }

  protected def transform(metadata: FileStoreFileMetadata): ObjectMetadata = {
    val result = new ObjectMetadata()
    result.addUserMetadata("name", metadata.name)
    result.addUserMetadata("fileType", metadata.fileType.toString.toLowerCase)
    result
  }

  protected def transform(s3ObjectSummary: S3ObjectSummary): FileStoreFileSummary = {
    FileStoreFileSummary(
      s3ObjectSummary.getKey,
      s3ObjectSummary.getETag,
      s3ObjectSummary.getSize,
      new DateTime(s3ObjectSummary.getLastModified)
    )
  }

  protected def listObjects(bucket: FileStoreBucket): List[FileStoreFileSummary] = {
    client.listObjects(new ListObjectsRequest().withBucketName(bucketName(bucket))).getObjectSummaries.asScala.toList map transform
  }
}

object S3FileStore extends LazyLogging {
  // map of mandant to client
  private val mandantClients: TrieMap[String, S3FileStore] = TrieMap()

  private val opts = new ClientConfiguration
  opts.setSignerOverride("S3SignerType")
  opts.setMaxConnections(1000)

  def apply(mandantConfiguration: MandantConfiguration, system: ActorSystem): S3FileStore = {
    mandantClients.getOrElseUpdate(mandantConfiguration.name, {
      // not initialized for this mandant yet
      implicit val executionContext = system.dispatcher

      val client = buildClient(mandantConfiguration)

      val fileStore = new S3FileStore(mandantConfiguration.name, client)

      fileStore.createBuckets map {
        _.fold(
          error => logger.error(s"Error creating buckets for ${mandantConfiguration.name}: ${error.message}"),
          _ => logger.debug(s"Created file store buckets for ${mandantConfiguration.name}")
        )
      }

      fileStore
    })
  }

  private def buildClient(mandantConfiguration: MandantConfiguration): AmazonS3 = {
    val credentials = new BasicAWSCredentials(mandantConfiguration.config.getString("s3.aws-access-key-id"), mandantConfiguration.config.getString("s3.aws-secret-acccess-key"))
    AmazonS3ClientBuilder.standard()
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withClientConfiguration(opts)
      .withEndpointConfiguration(new EndpointConfiguration(mandantConfiguration.config.getString("s3.aws-endpoint"), Regions.DEFAULT_REGION.getName()))
      .withPathStyleAccessEnabled(true)
      .build()
  }
}