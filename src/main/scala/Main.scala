import com.typesafe.config.{Config, ConfigFactory}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import play.api.libs.json.Reads._

import java.sql.{DriverManager, ResultSet}
import scala.jdk.CollectionConverters._
import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Try, Using}

case class Cat(name: String, color: String, age: String)

object Main extends App {

  val confFile = "application.conf"

  val config: Config = ConfigFactory.load(confFile)
  Class.forName(config.getString("jdbc.driver"))
  val url = config.getString("jdbc.url")
  val jsonFilePath = config.getString("dbTable.sourceFilePath")
  val tableName = config.getString("dbTable.name")
  val tableFields = config.getStringList("dbTable.fields").asScala.toArray

  val dbManager = DBManager(url)
  dbManager.createTable(tableName, tableFields) match {
    case Failure(exception) => throw exception
    case Success(_) => println(s"Table $tableName has been created successfully")
  }

  val parser: JsonParser = JsonParser(jsonFilePath)
  val dataToSave: Try[Seq[Cat]] = parser.getDataFromFile

  dataToSave match {
    case Success(data) => dbManager.saveToTable(tableName, data)
    case Failure(exception) => throw exception
  }

  dbManager.printTable(tableName)

}


case class JsonParser(jsonFilePath: String) {
  private val buffer: BufferedSource = Source.fromResource(jsonFilePath)
  private val json: JsValue = Json.parse(buffer.mkString)

  private implicit val catsReads: Reads[Cat] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "color").read[String] and
      (JsPath \ "color").read[String]
    ) (Cat.apply _)

  def getDataFromFile: Try[Seq[Cat]] = {
    json.validate[Seq[Cat]] match {
      case JsSuccess(value, _) => Success(value)
      case JsError(_) => Failure(new Exception("There is an error happened during json parsing"))
    }
  }
}

case class DBManager(url: String) {

  def createTable(tableName: String, tableFields: Array[String]): Try[Boolean] = {
    Using(DriverManager.getConnection(url).createStatement()) {
      st => {
        val sql = s"create table $tableName(${tableFields.map(_ + " VARCHAR(20)").mkString(",")})"
        st.execute(sql)
      }
    }
  }

  def saveToTable(tableName: String, toSave: Seq[Cat]): Unit = {
    Using(DriverManager.getConnection(url).createStatement()) {
      st => {
        for {
          cat <- toSave
        } yield {
          val sql = s"insert into $tableName values('${cat.name}', '${cat.color}')"
          st.execute(sql)
        }
      }
    }
  }

  def printTable(tableName: String): Unit = {
    Using(DriverManager.getConnection(url).createStatement()) {
      st => {
        println(s"Table $tableName:")
        val sql = s"select * from $tableName"
        val rs: ResultSet = st.executeQuery(sql)
        while (rs.next()) {
          println(s"Name: ${rs.getString("name")}, color: ${rs.getString("color")}")
        }
      }
    }
  }

}