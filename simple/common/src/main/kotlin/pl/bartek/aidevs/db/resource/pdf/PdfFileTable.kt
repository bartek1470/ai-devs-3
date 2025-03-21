package pl.bartek.aidevs.db.resource.pdf

import org.jetbrains.exposed.dao.id.UUIDTable
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

object PdfFileTable : UUIDTable("pdf_file") {
    val filePath =
        text("file_path")
            .uniqueIndex()
            .transform(
                wrap = { Path(it) },
                unwrap = { it.absolutePathString() },
            )
}
