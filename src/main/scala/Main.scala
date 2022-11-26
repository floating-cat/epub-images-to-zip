import java.nio.file.{FileSystems, Files, Path}
import scala.io.Source
import scala.util.Using
import scala.xml.parsing.XhtmlParser
import scala.xml.XML

import util.chaining.scalaUtilChainingOps

val containerXmlFilePath = "META-INF/container.xml"

@main def main(regexPattern: String, epubFolder: String): Unit =
  Path.of(epubFolder).findInCurrentDir(regexPattern).foreach(covert)

  def covert(epubPath: Path) =
    Using.resource(FileSystems.newFileSystem(epubPath)) { zipFs =>
      given Conversion[String, Path] = zipFs.getPath(_)
      given Conversion[Seq[String], Seq[Path]] = _.map(identity)
      val contentOpfPath = getContentOpfFile(containerXmlFilePath)
      val spineDocuments = getSpineDocuments(contentOpfPath)
      val images = getImages(spineDocuments)

      val zipLocation =
        epubPath.getParent.resolve("converted_files").resolve(epubPath.getFileName.toString.replace(".epub", ".zip"))
      createZipFile(zipLocation, images)
    }

  def getContentOpfFile(containerXmlPath: Path) =
    val containerXml = XML.loadString(Files.readString(containerXmlPath))
    (containerXml \ "rootfiles" \ "rootfile").find(n =>
      (n \@ "media-type") == "application/oebps-package+xml"
    ).get \@ "full-path"

  def getSpineDocuments(contentOpfPath: Path): Seq[String] =
    val contentOpfXml = XML.loadString(Files.readString(contentOpfPath))
    val manifestItemMap = (contentOpfXml \ "manifest" \ "item").map(n => (n \@ "id", n \@ "href")).toMap
    val spineIdRefs = ((contentOpfXml \ "spine").find(n => (n \@ "toc") == "ncx").get \ "itemref").map(_ \@ "idref")
    // remove the duplicated cover in some epub files
    (if spineIdRefs.head == "Page_cover" then spineIdRefs.tail else spineIdRefs)
      .map(idRef => manifestItemMap(idRef).tap(_ != null))

  def getImages(documentPaths: Seq[Path]): Seq[String] =
    documentPaths.flatMap { path =>
      val htmlXml = XhtmlParser(Source.fromString(Files.readString(path)))
      (htmlXml \\ "img").map(_ \@ "src")
    }

  def createZipFile(outputLocation: Path, imagePaths: Seq[Path]) =
    Files.createDirectories(outputLocation.getParent)
    Using.resource(FileSystems.newFileSystem(outputLocation, java.util.Map.of("create", "true"))) { zipFs =>
      val fileNumberLength = (imagePaths.length - 1).toString.length

      imagePaths.foldLeft(0) { (number, path) =>
        // TOOD: JDK 20 getExtension()
        Files.copy(path, zipFs.getPath(String.format(s"%${fileNumberLength}d", number) + s".${path.extension}"))
        number + 1
      }
    }
