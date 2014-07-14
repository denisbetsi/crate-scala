import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest._

import org.elasticsearch.transport.RemoteTransportException

import io.crate.action.sql.SQLActionException
import io.crate.action.sql.SQLResponse

class CrateClientSpec extends FlatSpec with Matchers {

  val timeout = 5 seconds

  val timestamp = new java.util.Date().getTime()

  val client = CrateClient("localhost:4300")

  "Crate Client" should "create new client" in {
    val request = client.sql("SELECT * FROM sys.nodes")
    val response = Await.result(request, timeout)
    assert(response.cols.length > 0)
  }

  it should "drop table" in {
    val request = client.sql("DROP TABLE foo")
    val response = Await.result(request, timeout)
    println("drop table: " + response)
  }

  it should "create table" in {
    val request = client.sql("CREATE TABLE foo (id int primary key, name string)")
    val response = Await.result(request, timeout)
    println("create table: " + response)
  }

  it should "produce SQLActionException when creating existing table" in {
    val thrown = intercept[RemoteTransportException] {
       Await.result(client.sql("CREATE TABLE foo (id int primary key, name string)"), timeout)
    }
    println(thrown)
    println(thrown.getCause())
    thrown.getCause() shouldBe a [SQLActionException]
  }

  it should "insert data with parameter substitution" in {
    val stmt = "INSERT INTO foo (id, name) VALUES (?, ?)"
    val args = Array(1, "bar")
    val sqlRequest = SQLRequest(stmt, args)
    val request = client.sql(sqlRequest)
    val response = Await.result(request, timeout)
    println("insert into: " + response)
    response.rowCount shouldBe (1)
  }

  it should "map Scala to Java data types on insertion" in {
    val dropResult = Await.result(client.sql("DROP TABLE test"), timeout)
    val result = Await.result(client.sql("CREATE TABLE test (st string, sh short, i integer, lo long, fl float, do double, b byte, bo boolean, arr array(string), o object, a ip, ts timestamp, ge geo_point)"), timeout)
    println("create table: " + result)

    val stmt = "INSERT INTO test (st, sh, i, lo, fl, do, b, bo, arr, o, a, ts, ge) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    val args = Array(
      "hello",
      Short.MaxValue,
      Int.MaxValue,
      Long.MaxValue,
      Float.MaxValue,
      Double.MaxValue,
      Byte.MaxValue,
      true,
      Array("crate", "is", "pretty", "cool"),
      Map("nested" -> true, "maps" -> "yes"),
      "127.0.0.1",
      timestamp,
      Array(-0.1015987, 51.5286416)
    )

    val sqlRequest = SQLRequest(stmt, args)
    val request = client.sql(sqlRequest)
    val response = Await.result(request, timeout)
    println("insert into: " + response)
    response.rowCount shouldBe (1)
  }

  it should "map Scala to Java data types for arrays" in {
    val dropResult = Await.result(client.sql("DROP TABLE testarrays"), timeout)
    val result = Await.result(client.sql("CREATE TABLE testarrays (a_string array(string), a_short array(short), a_integer array(integer), a_long array(long), a_float array(float), a_double array(double), a_byte array(byte), a_boolean array(boolean), a_object array(object), a_ip array(ip), a_timestamp array(timestamp))"), timeout)
    println("create table: " + result)

    val stmt = "INSERT INTO testarrays (a_string, a_short, a_integer, a_long, a_float, a_double, a_byte, a_boolean, a_ip, a_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    val args = Array(
      Array("hello"),
      Array(Short.MaxValue),
      Array(Int.MaxValue),
      Array(Long.MaxValue),
      Array(Float.MaxValue),
      Array(Double.MaxValue),
      Array(Byte.MaxValue),
      Array(true),
      Array("127.0.0.1"),
      Array(timestamp)
    )

    val sqlRequest = SQLRequest(stmt, args)
    val request = client.sql(sqlRequest)
    val response = Await.result(request, timeout)
    println("insert into: " + response)
    response.rowCount shouldBe (1)
  }

  it should "map Java to Scala data types on select" in {
    refresh("test")
    val request = client.sql("SELECT * FROM test")
    val response = Await.result(request, timeout)
    println("select: " + response)
    response.rowCount shouldBe (1)

    val rows = response.rows
    val row = rows(0)
    row shouldBe a [Array[Any]]

    // result columns are alphabetically sorted
    row(0) shouldBe a [String]
    row(0) shouldBe "127.0.0.1"
    row(1) shouldBe a [List[_]]
    row(1).asInstanceOf[List[String]] should contain inOrderOnly ("crate", "is", "pretty", "cool")
    //row(2) shouldBe a [Byte] // CRATE: 127 was not an instance of byte, but an instance of java.lang.Integer
    row(2) shouldBe Byte.MaxValue
    //row(3) shouldBe a [Boolean]
    row(3) shouldBe true
    //row(4) shouldBe a [Double]
    row(4) shouldBe Double.MaxValue
    //row(5) shouldBe a [Float] // CRATE: 3.4028235E38 was not an instance of float, but an instance of java.lang.Double
    //row(5) shouldBe Float.MaxValue // CRATE: 3.4028235E38 was not equal to 3.4028235E38
    row(6) shouldBe a [List[_]]
    row(6).asInstanceOf[List[Double]] should contain inOrderOnly (-0.1015987, 51.5286416)
    //row(7) shouldBe a [Int]
    row(7) shouldBe Int.MaxValue
    //row(8) shouldBe a [Long]
    row(8) shouldBe Long.MaxValue
    row(9).asInstanceOf[Map[_, _]] should contain only ("nested" -> true, "maps" -> "yes")
    //row(10) shouldBe a [Short] // CRATE: 32767 was not an instance of short, but an instance of java.lang.Integer
    row(10) shouldBe Short.MaxValue
    row(11) shouldBe a [String]
    row(11) shouldBe "hello"
    //row(12) shouldBe a [Long]
    row(12) shouldBe timestamp
  }

  // sqlRequest.includeTypesOnResponse(true)

  def refresh(table: String) = Await.ready(client.sql("refresh table " + table), timeout)

}