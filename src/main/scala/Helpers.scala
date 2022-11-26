import java.nio.file.{Files, Path}
import scala.jdk.StreamConverters.StreamHasToScala

extension (path: Path)
  def findInCurrentDir(regexPattern: String): Seq[Path] =
    Files.find(path, 1, (path, _) => path.getFileName.toString.matches(regexPattern))
      .toScala(Seq)

  def extension: String =
    val filename = path.getFileName.toString
    val i = filename.lastIndexOf('.')
    if i >= 0 then
      filename.substring(i + 1)
    else
      ""
