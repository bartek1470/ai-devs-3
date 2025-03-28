package pl.bartek.aidevs.db.resource.pdf

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import pl.bartek.aidevs.db.pdf.BasePdfResourceTable
import java.util.UUID

abstract class BasePdfResource(
    id: EntityID<UUID>,
    table: BasePdfResourceTable,
) : UUIDEntity(id) {
    var name by table.name
    var pages by table.pages
    var hash by table.hash
    var pdfFile by PdfFile referencedOn table.pdfFile

    override fun toString(): String = "BasePdfResource(name='$name', hash='$hash', pdfFile=$pdfFile)"
}
