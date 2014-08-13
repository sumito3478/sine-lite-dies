package sine.lite.dies

import java.time.ZonedDateTime
import org.eclipse.jgit.lib._

case class Content(body: String, metaData: Map[String, String], tags: List[String], title: String, permalink: String, updatedAt: ZonedDateTime, createdAt: ZonedDateTime, urn: String)
