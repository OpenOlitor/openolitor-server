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
package ch.openolitor.util.jsonpath

import org.specs2.mutable._
import spray.json._

/**
 * Originally token from gatlin-jsonpath and converted to spray-json
 */
class JsonPathSpec extends Specification {

  private def parseJson(s: String): JsValue = parseJson(s)
  private def bool(b: Boolean) = JsBoolean(b)
  private def int(i: Int) = JsNumber(i)
  private def double(f: Double) = JsNumber(f)
  private def string(s: String): JsString = JsString(s)
  private def nullNode: Any = JsNull
  //  def array(elts: Any*): JList[Any] = elts.asJava
  //  def obj(elts: (String, Any)*) = {
  //    val node = new ObjectNode
  //    elts.foreach { case (key, value) =>
  //      node.put(key, value)
  //    }
  //    node
  //  }

  // Goessner JSON exemple

  private val book1 = """{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95}"""
  private val book2 = """{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99}"""
  private val book3 = """{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99}"""
  private val book4 = """{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}"""
  private val allBooks = s"[$book1,$book2,$book3,$book4]"
  private val bicycle = s"""{"color":"red","price":19.95}"""
  private val allStore = s"""{"book":$allBooks, "bicycle":$bicycle}"""
  private val goessnerData = s"""{"store":$allStore}"""
  private val goessnerJson = parseJson(goessnerData)

  private val json = """[
                       |    {
                       |        "id":19434,
                       |        "foo":1,
                       |        "company":
                       |        {
                       |            "id":18971
                       |        },
                       |        "owner":
                       |        {
                       |            "id":18957
                       |        },
                       |        "process":
                       |        {
                       |            "id":18972
                       |        }
                       |    },
                       |    {
                       |        "id":19435,
                       |        "foo":2,
                       |        "company":
                       |        {
                       |            "id":18972
                       |        },
                       |        "owner":
                       |        {
                       |            "id":18957
                       |        },
                       |        "process":
                       |        {
                       |            "id":18974
                       |        }
                       |    }
                       |]""".stripMargin

  private val veggies = """[
                          |    {
                          |        "vegetable": {
                          |            "name": "peas",
                          |            "color": "green"
                          |        },
                          |        "meet": {
                          |            "name":"beef",
                          |            "color":"red"
                          |        }
                          |    },
                          |    {
                          |        "vegetable": {
                          |            "name": "potato",
                          |            "color": "yellow"
                          |        },
                          |        "meet": {
                          |            "name":"lamb",
                          |            "color":"brown"
                          |        }
                          |    }
                          |]""".stripMargin

  private val searches = """[
                           |  {
                           |    "changes": [
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "520"
                           |        },
                           |        [
                           |          "0",
                           |          {
                           |            "id": "520",
                           |            "location": "foo",
                           |            "v": {
                           |              "action": ""
                           |            }
                           |          },
                           |          [
                           |            "actions",
                           |            {
                           |
                           |            },
                           |            [
                           |              "action",
                           |              {
                           |                "key": "1",
                           |                "kc": 81,
                           |                "mk": [
                           |                  "18",
                           |                  "16",
                           |                  "17"
                           |                ]
                           |              }
                           |            ],
                           |            [
                           |              "action",
                           |              {
                           |                "key": "2",
                           |                "kc": 13,
                           |                "mk": [
                           |
                           |                ]
                           |              }
                           |            ],
                           |            [
                           |              "action",
                           |              {
                           |                "key": "3",
                           |                "kc": 13,
                           |                "mk": [
                           |
                           |                ]
                           |              }
                           |            ]
                           |          ]
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "1012"
                           |        },
                           |        [
                           |          "23",
                           |          {
                           |            "id": "1012",
                           |            "multiselectmode": 1,
                           |            "selectmode": "multi",
                           |            "cols": 13,
                           |            "rows": 19,
                           |            "firstrow": 0,
                           |            "totalrows": 20,
                           |            "pagelength": 19,
                           |            "colheaders": true,
                           |            "colfooters": false,
                           |            "vcolorder": [
                           |              "1",
                           |              "2",
                           |              "3",
                           |              "4",
                           |              "5",
                           |              "6",
                           |              "7",
                           |              "8",
                           |              "9",
                           |              "10",
                           |              "11",
                           |              "12",
                           |              "13"
                           |            ],
                           |            "pb-ft": 0,
                           |            "pb-l": 18,
                           |            "clearKeyMap": true,
                           |            "v": {
                           |              "selected": [
                           |
                           |              ],
                           |              "firstvisible": 0,
                           |              "sortcolumn": "null",
                           |              "sortascending": true,
                           |              "reqrows": -1,
                           |              "reqfirstrow": -1,
                           |              "columnorder": [
                           |                "1",
                           |                "2",
                           |                "3",
                           |                "4",
                           |                "5",
                           |                "6",
                           |                "7",
                           |                "8",
                           |                "9",
                           |                "10",
                           |                "11",
                           |                "12",
                           |                "13"
                           |              ],
                           |              "collapsedcolumns": [
                           |
                           |              ],
                           |              "noncollapsiblecolumns": [
                           |                "1"
                           |              ]
                           |            }
                           |          },
                           |          [
                           |            "rows",
                           |            {
                           |
                           |            },
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 191,
                           |                "style-4": "perfectMatch"
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 192,
                           |                "style-5": "perfectMatch",
                           |                "style-10": "perfectMatch"
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 193,
                           |                "style-4": "perfectMatch",
                           |                "style-5": "perfectMatch",
                           |                "style-10": "perfectMatch"
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 194,
                           |                "style-4": "perfectMatch",
                           |                "style-10": "perfectMatch"
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 195,
                           |                "style-4": "perfectMatch"
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 196
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 197
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 198
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 199
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 200
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 201
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 202
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 203
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 204,
                           |                "style-4": "perfectMatch",
                           |                "style-10": "perfectMatch"
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 205,
                           |                "style-4": "perfectMatch",
                           |                "style-10": "perfectMatch"
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 206,
                           |                "style-4": "perfectMatch",
                           |                "style-10": "perfectMatch"
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 207,
                           |                "style-4": "perfectMatch",
                           |                "style-10": "perfectMatch"
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 208,
                           |                "style-4": "perfectMatch",
                           |                "style-10": "perfectMatch"
                           |              },
                           |              ""
                           |            ],
                           |            [
                           |              "tr",
                           |              {
                           |                "key": 209
                           |              },
                           |              ""
                           |            ]
                           |          ],
                           |          [
                           |            "visiblecolumns",
                           |            {
                           |
                           |            },
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "1",
                           |                "caption": "foo",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "2",
                           |                "caption": "bar",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "3",
                           |                "caption": "baz",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "4",
                           |                "caption": "too",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "5",
                           |                "caption": "xxx",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "6",
                           |                "caption": "yyy",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "7",
                           |                "caption": "zzz",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "8",
                           |                "caption": "aaa",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "9",
                           |                "caption": "bbb",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "10",
                           |                "caption": "ccc",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "11",
                           |                "caption": "ddd2",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "12",
                           |                "caption": "eee",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ],
                           |            [
                           |              "column",
                           |              {
                           |                "cid": "13",
                           |                "caption": "fff",
                           |                "fcaption": "",
                           |                "sortable": true
                           |              }
                           |            ]
                           |          ]
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "997"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "997"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "1011"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "1011"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "996"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "996"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "994"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "994"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "999"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "999"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "993"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "993"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "995"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "995"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "1003"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "1003"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "990"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "990"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "992"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "992"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "1005"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "1005"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "1001"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "1001"
                           |          }
                           |        ]
                           |      ],
                           |      [
                           |        "change",
                           |        {
                           |          "pid": "991"
                           |        },
                           |        [
                           |          "1",
                           |          {
                           |            "id": "991"
                           |          }
                           |        ]
                           |      ]
                           |    ],
                           |    "state": {
                           |      "520": {
                           |        "pollInterval": -1
                           |      },
                           |      "1011": {
                           |        "text": "(20)"
                           |      }
                           |    },
                           |    "types": {
                           |      "520": "0",
                           |      "990": "1",
                           |      "991": "1",
                           |      "992": "1",
                           |      "993": "1",
                           |      "994": "1",
                           |      "995": "1",
                           |      "996": "1",
                           |      "997": "1",
                           |      "999": "1",
                           |      "1001": "1",
                           |      "1003": "1",
                           |      "1005": "1",
                           |      "1011": "1",
                           |      "1012": "23"
                           |    },
                           |    "hierarchy": {
                           |      "520": [
                           |        "983",
                           |        "521",
                           |        "984"
                           |      ],
                           |      "990": [
                           |
                           |      ],
                           |      "991": [
                           |
                           |      ],
                           |      "992": [
                           |
                           |      ],
                           |      "993": [
                           |
                           |      ],
                           |      "994": [
                           |
                           |      ],
                           |      "995": [
                           |
                           |      ],
                           |      "996": [
                           |
                           |      ],
                           |      "997": [
                           |
                           |      ],
                           |      "999": [
                           |
                           |      ],
                           |      "1001": [
                           |
                           |      ],
                           |      "1003": [
                           |
                           |      ],
                           |      "1005": [
                           |
                           |      ],
                           |      "1011": [
                           |
                           |      ],
                           |      "1012": [
                           |
                           |      ]
                           |    },
                           |    "rpc": [
                           |
                           |    ],
                           |    "meta": {
                           |
                           |    },
                           |    "resources": {
                           |
                           |    },
                           |    "timings": [
                           |      4509,
                           |      1
                           |    ]
                           |  }
                           |]""".stripMargin

  private val valuesWithParensAndBraces =
    """{
      |  "error": {
      |     "id": 1,
      |    "message1": "bar(baz)",
      |    "message2": "bar[baz]"
      |  }
      |}""".stripMargin

  //////////////

  "Incorrect JsonPath expressions" should {
    "be handled properly" in {
      JsonPath.query("€.$", goessnerJson) === 'left
    }
  }

  "Keys starting with number" should {
    "be handled properly" in {
      val json = parseJson(""" {"a": "b", "2": 2, "51a": "t"} """)
      JsonPath.query("$.2", json) === Right(Vector(int(2)))
      JsonPath.query("$.51a", json) === Right(Vector(string("t")))
    }
  }

  "Support of Goessner test cases" should {
    "work with test set 1" in {
      val json = parseJson("""{"a":"a","b":"b","c d":"e"}""")
      JsonPath.query("$.a", json) === Right(Vector(string("a")))
      JsonPath.query("$['a']", json) === Right(Vector(string("a")))
      // Not supported syntax "$.'c d'", here is an alternative to it
      JsonPath.query("$['c d']", json) === Right(Vector(string("e")))
      JsonPath.query("$.*", json) === Right(Vector(string("a"), string("b"), string("e")))
      JsonPath.query("$['*']", json) === Right(Vector(string("a"), string("b"), string("e")))
      // Not supported syntax "$[*]" ... shouldn't that operator only apply on arrays ?
    }
  }

  "it" should {
    "work with test set 2" in {
      val json = parseJson("""[ 1, "2", 3.14, true, null ]""")
      JsonPath.query("$[0]", json) === Right(Vector(int(1)))
      JsonPath.query("$[4]", json) === Right(Vector(nullNode))
      JsonPath.query("$[*]", json) === Right(Vector(
        int(1),
        string("2"),
        double(3.14),
        bool(true),
        nullNode
      ))
      JsonPath.query("$[-1:]", json) === Right(Vector(nullNode))
    }

    "work with test set 3" in {
      val json = parseJson(
        """{ "points": [
          				             { "id":"i1", "x": 4, "y":-5 },
          				             { "id":"i2", "x":-2, "y": 2, "z":1 },
          				             { "id":"i3", "x": 8, "y": 3 },
          				             { "id":"i4", "x":-6, "y":-1 },
          				             { "id":"i5", "x": 0, "y": 2, "z":1 },
          				             { "id":"i6", "x": 1, "y": 4 }
          				           ]
          				         }"""
      )

      JsonPath.query("$.points[1]", json) === Right(Vector(parseJson("""{ "id":"i2", "x":-2, "y": 2, "z":1 }""")))
      JsonPath.query("$.points[4].x", json) === Right(Vector(int(0)))
      JsonPath.query("$.points[?(@['id']=='i4')].x", json) === Right(Vector(int(-6)))
      JsonPath.query("$.points[*].x", json) === Right(Vector(int(4), int(-2), int(8), int(-6), int(0), int(1)))
      // Non supported syntax "$['points'][?(@['x']*@['x']+@['y']*@['y'] > 50)].id"
      JsonPath.query("$['points'][?(@['y'] >= 3)].id", json) === Right(Vector(string("i3"), string("i6")))
      JsonPath.query("$.points[?(@['z'])].id", json) === Right(Vector(string("i2"), string("i5")))
      // Non supported syntax "$.points[(count(@)-1)].id"
    }

    "work with boolean filters" in {
      val json = parseJson(
        """{ "conditions":
          			[true, false, true]
          		}"""
      )

      JsonPath.query("$.conditions[?(@ == true)]", json) === Right(Vector(bool(true), bool(true)))
      JsonPath.query("$.conditions[?(@ == false)]", json) === Right(Vector(bool(false)))
      JsonPath.query("$.conditions[?(false == @)]", json) === Right(Vector(bool(false)))
    }

    "work with nested boolean filters" in {
      val json = parseJson(
        """{ "conditions":
          			[
          				{ "id": "i1", "condition": true },
          				{ "id": "i2", "condition": false }
          			]
          		}"""
      )

      JsonPath.query("$.conditions[?(@['condition'] == true)].id", json) === Right(Vector(string("i1")))
      JsonPath.query("$.conditions[?(@['condition'] == false)].id", json) === Right(Vector(string("i2")))
    }

    "work with nested objects" in {
      val json = parseJson(""" { "foo" : {"bar" : "baz"} }""")
      val x = JsonPath.query("$.foo", json)
      val expected = new JsObject(Map("bar" -> JsString("baz")))
      x === Right((expected))
      JsonPath.query("$.foo.bar", json) === Right(Vector(string("baz")))
      JsonPath.query("$..bar", json) === Right(Vector(string("baz")))
    }

    "work with arrays" in {
      val json = parseJson("""{"foo":[{"lang":"en"},{"lang":"fr"}]}""")
      JsonPath.query("$.foo[*].lang", json) === Right(Vector(string("en"), string("fr")))
    }

    "work with null elements when fetching node" in {
      val json = parseJson("""{"foo":null}""")
      JsonPath.query("$.foo", json) === Right(Vector(nullNode))
    }

    "work with null elements when fetching children" in {
      val json = parseJson("""{"foo":null}""")
      JsonPath.query("$.foo[*]", json) === Right(Vector())
    }

    "work with nested arrays" in {
      val json = parseJson("""[[{"foo":1}]]""")
      JsonPath.query("$.foo", json) === Right(Vector())
      JsonPath.query("$..foo", json) === Right(Vector(int(1)))
      JsonPath.query("$[0][0].foo", json) === Right(Vector(int(1)))
    }

    "work when the slice operator has one separator" in {
      JsonPath.query("$[:-1]", goessnerJson) === Right(Vector())
      JsonPath.query("$[:]", ten) === Right(Vector(
        int(1),
        int(2),
        int(3),
        int(4),
        int(5),
        int(6),
        int(7),
        int(8),
        int(9),
        int(10)
      ))
      JsonPath.query("$[7:]", ten) === Right(Vector(int(8), int(9), int(10)))
      JsonPath.query("$[-2:]", ten) === Right(Vector(int(9), int(10)))
      JsonPath.query("$[:3]", ten) === Right(Vector(int(1), int(2), int(3)))
      JsonPath.query("$[:-7]", ten) === Right(Vector(int(1), int(2), int(3)))
      JsonPath.query("$[3:6]", ten) === Right(Vector(int(4), int(5), int(6)))
      JsonPath.query("$[-5:-2]", ten) === Right(Vector(int(6), int(7), int(8)))
    }

    "work when the slice operator has two separators" in {
      JsonPath.query("$[:6:2]", ten) === Right(Vector(int(1), int(3), int(5)))
      JsonPath.query("$[1:9:3]", ten) === Right(Vector(int(2), int(5), int(8)))
      JsonPath.query("$[:5:-1]", ten) === Right(Vector(int(10), int(9), int(8), int(7)))
      JsonPath.query("$[:-4:-1]", ten) === Right(Vector(int(10), int(9), int(8)))
      JsonPath.query("$[3::-1]", ten) === Right(Vector(int(4), int(3), int(2), int(1)))
      JsonPath.query("$[-8::-1]", ten) === Right(Vector(int(3), int(2), int(1)))
    }

    "work with a deep subquery" in {
      val json2 = parseJson("""{"all":[{"foo":{"bar":1,"baz":2}},{"foo":3}]}""")
      JsonPath.query("$.all[?(@.foo.bar)]", json2) === Right(Vector(parseJson("""{"foo":{"bar":1,"baz":2}}""")))
    }

    "pick only proper node" in {
      val json3 = parseJson("""{ "foo":{"bar":1, "baz":2}, "second":{"bar":3} }""")
      JsonPath.query("$..[?(@.baz)]", json3) === Right(Vector(parseJson("""{"bar":1,"baz":2}""")))
    }

    "return only one result with object nested in object 1" in {
      JsonPath.query("""$..state..[?(@.text == "(20)")].text""", parseJson(searches)) === Right(Vector(string("(20)")))
    }

    "return only one result with object nested in object 2" in {
      JsonPath.query("""$..[?(@.text == "(20)")].text""", parseJson(searches)) === Right(Vector(string("(20)")))
    }

    "work with some boolean operators" in {
      val oneToFive = parseJson("[1,2,3,4,5]")
      JsonPath.query("$[?(@ > 3)]", oneToFive) === Right(Vector(int(4), int(5)))
      JsonPath.query("$[?(@ == 3)]", oneToFive) === Right(Vector(int(3)))

      val json = parseJson("""[{"foo":"a"},{"foo":"b"},{"bar":"c"}]""")

      val expected = new JsObject(Map("foo" -> JsString("a")))

      JsonPath.query("$[?(@.foo=='a' )]", json) === Right(Vector(expected))
    }

    "work with non-alphanumeric values" in {
      val json = parseJson("""{ "a":[{ "a":5, "@":2, "$":5 },
            						              { "a":6, "@":3, "$":4 },
            						              { "a":7, "@":4, "$":5 }
            						             ]}""")
      JsonPath.query("""$.a[?(@['@']==3)]""", json) === Right(Vector(parseJson("""{"a":6,"@":3,"$":4}""")))
      JsonPath.query("""$.a[?(@['$']!=5)]""", json) === Right(Vector(parseJson("""{"a":6,"@":3,"$":4}""")))
    }

    "work with some predefined comparison operators" in {
      val oneToSeven = parseJson("[1,2,3,4,5,6,7]")
      JsonPath.query("$[0][?(@>1)]", oneToSeven) === Right(Vector())
      JsonPath.query("$[?( @>1 && @<=4 )]", oneToSeven) === Right(Vector(int(2), int(3), int(4)))
      JsonPath.query("$[?( @>6 && @<2 || @==3 || @<=4 && @>=4 )]", oneToSeven) === Right(Vector(int(3), int(4)))
      JsonPath.query("$[?( @==7 || @<=4 && @>1)]", oneToSeven) === Right(Vector(int(2), int(3), int(4), int(7)))
      JsonPath.query("$[?( @==1 || @>4 )]", oneToSeven) === Right(Vector(int(1), int(5), int(6), int(7)))
    }

    "support reference to the root-node" in {
      val authors = """[{"pseudo":"Tolkien","name": "J. R. R. Tolkien"},{"pseudo":"Hugo","name":"Victor Hugo"}]"""
      val library = parseJson(s"""{"book":$allBooks,"authors":$authors}""")

      JsonPath.query("""$.authors[?(@.pseudo=='Tolkien')].name""", library) === Right(Vector(string("J. R. R. Tolkien")))

      JsonPath.query("""$.book[?(@.author==$.authors[?(@.pseudo=='Tolkien')].name)].title""", library) === Right(Vector(string("The Lord of the Rings")))
      JsonPath.query("""$.book[?(@.author==$.authors[?(@.pseudo=='Hugo')].name)].title""", library) === Right(Vector())
    }

    "honor current object" in {
      JsonPath.query("""$..vegetable[?(@.color=='green')].name""", parseJson(veggies)) === Right(Vector(string("peas")))
    }

    "not mess up with node with the same name at different depths in the hierarchy" in {
      val json = """{"foo":{"nico":{"nico":42}}}"""
      JsonPath.query("""$..foo[?(@.nico)]""", parseJson(json)) === Right(Vector(parseJson("""{"nico":{"nico":42}}}""")))
    }

    "work with getting the whole store" in {
      JsonPath.query("$..book.*", goessnerJson) === Right(Vector())
      JsonPath.query("$.store.*", goessnerJson) === Right(Vector(parseJson(allBooks), parseJson(bicycle)))
    }

    "work with getting all prices" in {
      JsonPath.query("$.store..price", goessnerJson) === Right(Vector(
        double(8.95),
        double(12.99),
        double(8.99),
        double(22.99),
        double(19.95)
      ))
    }

    "work with getting books by indices" in {
      JsonPath.query("$..book[2]", goessnerJson) === Right(Vector(parseJson(book3)))
      JsonPath.query("$..book[-1:]", goessnerJson) === Right(Vector(parseJson(book4)))
      JsonPath.query("$..book[0,1]", goessnerJson) === Right(Vector(parseJson(book1), parseJson(book2)))
      JsonPath.query("$..book[:2]", goessnerJson) === Right(Vector(parseJson(book1), parseJson(book2)))
    }

    "allow to get everything" in {
      JsonPath.query("$..*", goessnerJson) === Right(Vector(
        goessnerJson,
        parseJson(allStore),
        parseJson(bicycle),
        string("red"),
        double(19.95),
        parseJson(allBooks),
        parseJson(book1),
        parseJson(book2),
        parseJson(book3),
        parseJson(book4),
        string("Nigel Rees"),
        string("Sayings of the Century"),
        string("reference"),
        double(8.95),
        string("Evelyn Waugh"),
        string("Sword of Honour"),
        string("fiction"),
        double(12.99),
        string("Herman Melville"),
        string("Moby Dick"),
        string("fiction"),
        double(8.99),
        string("0-553-21311-3"),
        string("J. R. R. Tolkien"),
        string("The Lord of the Rings"),
        string("fiction"),
        double(22.99),
        string("0-395-19395-8")
      ))
    }

    "work with subscript filters" in {
      JsonPath.query("$..book[?(@.isbn)]", goessnerJson) === Right(Vector(
        parseJson(book3),
        parseJson(book4)
      ))
      JsonPath.query("$..book[?(@.isbn)].title", goessnerJson) === Right(Vector(
        string("Moby Dick"),
        string("The Lord of the Rings")
      ))
      JsonPath.query("$.store.book[?(@.category == 'fiction')].title", goessnerJson) === Right(Vector(
        string("Sword of Honour"),
        string("Moby Dick"),
        string("The Lord of the Rings")
      ))
      JsonPath.query("$.store.book[?(@.price < 20 && @.price > 8.96)].title", goessnerJson) === Right(Vector(
        string("Sword of Honour"),
        string("Moby Dick")
      ))

    }

    "honor recursive filters from root" in {
      JsonPath.query("$..*[?(@.id==19434 && @.foo==1)].foo", parseJson(json)) === Right(Vector(int(1)))
    }

    "honor recursive filter from root + recursive field" in {
      JsonPath.query("""$..[?(@.selectmode)]..id""", parseJson(searches)) === Right(Vector(string("1012")))
    }

    "honor recursive filter from root + field" in {
      JsonPath.query("""$..[?(@.selectmode)].id""", parseJson(searches)) === Right(Vector(string("1012")))
    }

    "honor recursive filter with wildcard from root + field" in {
      JsonPath.query("""$..*[?(@.selectmode)].id""", parseJson(searches)) === Right(Vector(string("1012")))
    }

    "honor recursive filter with wildcard from root + recursive field" in {
      JsonPath.query("""$..*[?(@.selectmode)]..id""", parseJson(searches)) === Right(Vector(string("1012")))
    }

    "honor deep array access filter" in {
      JsonPath.query("""$..changes[?(@[2][1].selectmode)][2][1].id""", parseJson(searches)) === Right(Vector(string("1012")))
    }

    "work fine when filter contains parens" in {
      JsonPath.query("""$..*[?(@.message1=='bar(baz)')].id""", parseJson(valuesWithParensAndBraces)) === Right(Vector(int(1)))
    }

    "work fine when filter contains square braces" in {
      JsonPath.query("""$..*[?(@.message2=='bar[baz]')].id""", parseJson(valuesWithParensAndBraces)) === Right(Vector(int(1)))
    }

  }

  "`null` elements" should {
    "be correctly handled" in {
      //    val fooNull = parseJson("""{"foo":null}""")
      //    JsonPath.query("$.foo", fooNull) should findElements(NullNode.instance)
      //    JsonPath.query("$.foo.bar", fooNull) should findElements()

      val arrayWithNull = parseJson("""{"foo":[1,null,3,"woot"]}""")
      JsonPath.query("$.foo[?(@==null)]", arrayWithNull) === Right(Vector(nullNode))
      //    JsonPath.query("$.foo[?(@>=null)]", arrayWithNull) should findElements()
      //    JsonPath.query("$.foo[?(@>=0.5)]", arrayWithNull) should findOrderedElements(int(1), int(3))
    }
  }

  "Field accessors" should {
    "work with a simple object" in {
      val json = parseJson("""{"foo" : "bar"}""")
      JsonPath.query("$.*", json) === Right(Vector(string("bar")))
      JsonPath.query("$.foo", json) === Right(Vector(string("bar")))
      JsonPath.query("$..foo", json) === Right(Vector(string("bar")))
      JsonPath.query("$.bar", json) === Right(Vector())
    }
  }

  "Multi-fields accessors" should {
    "be interpreted correctly" in {
      val json = parseJson("""{"menu":{"year":2013,"file":"open","options":[{"bold":true},{"font":"helvetica"},{"size":3}]}}""")
      JsonPath.query("$.menu['file','year']", json) === Right(Vector(string("open"), int(2013)))
      JsonPath.query("$..options['foo','bar']", json) === Right(Vector())
      JsonPath.query("$..options[*]['bold','size']", json) === Right(Vector(bool(true), int(3)))
    }
  }

  private val ten = parseJson("[1,2,3,4,5,6,7,8,9,10]")

  "Array field slicing" should {
    "work with random accessors" in {
      JsonPath.query("$[0]", goessnerJson) === Right(Vector())
      JsonPath.query("$[0]", ten) === Right(Vector(int(1)))
      JsonPath.query("$[-1]", ten) === Right(Vector(int(10)))
      JsonPath.query("$[9]", ten) === Right(Vector(int(10)))
      JsonPath.query("$[2,7]", ten) === Right(Vector(int(3), int(8)))
      JsonPath.query("$[2,-7]", ten) === Right(Vector(int(3), int(4)))
      JsonPath.query("$[2,45]", ten) === Right(Vector(int(3)))
    }
  }

  "Filters" should {
    "be applied on array children and pick all matching ones" in {
      val json = parseJson("""[{"foo":1},{"foo":2},{"bar":3}]""")

      val expected1 = new JsObject(Map("foo" -> int(1)))
      val expected2 = new JsObject(Map("foo" -> int(2)))

      JsonPath.query("$[?(@.foo)]", json) === Right(Vector(expected1, expected2))
    }
  }

  "empty String value" should {
    "be correctly handled" in {
      val emptyStringValue = parseJson("""[{"foo": "bar", "baz": ""}]""")
      JsonPath.query("$[?(@.baz == '')].foo", emptyStringValue) === Right(Vector(string("bar")))
    }
  }

  /// Goessner reference examples ///////////////////////////////////////////

  "Goessner examples" should {

    "work with finding all the authors" in {

      JsonPath.query("$.store.book[*].author", goessnerJson) === Right(Vector(
        string("Nigel Rees"),
        string("Evelyn Waugh"),
        string("Herman Melville"),
        string("J. R. R. Tolkien")
      ))
      JsonPath.query("$..author", goessnerJson) === Right(Vector(
        string("Nigel Rees"),
        string("Evelyn Waugh"),
        string("Herman Melville"),
        string("J. R. R. Tolkien")
      ))
    }
  }

  "Recursive" should {
    "honor filters directly on root" in {
      JsonPath.query("$[?(@.id==19434 && @.foo==1)].foo", parseJson(json)) === Right(Vector(int(1)))
    }
  }

  "Searches" should {
    "honor recursive field + recursive filter + recursive field" in {
      JsonPath.query("""$..changes..[?(@.selectmode)]..id""", parseJson(searches)) === Right(Vector(string("1012")))
    }
  }

}
