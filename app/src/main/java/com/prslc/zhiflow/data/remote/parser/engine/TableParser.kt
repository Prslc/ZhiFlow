package com.prslc.zhiflow.data.remote.parser.engine

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.content.Table
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.data.remote.parser.model.ProcessedText

@Immutable
object TableParser {
    fun parse(
        table: Table,
        contentParser: (String) -> ProcessedText
    ): RichTextElement.Table {
        return RichTextElement.Table(
            rows = table.rowCount,
            cols = table.columnCount,
            cells = table.cells.map { cellText ->
                val processed = contentParser(cellText)
                RichTextElement.TableCell(processed.content, processed.inlineMetas)
            },
            hasHeader = table.hasHeadRow,
        )
    }
}
