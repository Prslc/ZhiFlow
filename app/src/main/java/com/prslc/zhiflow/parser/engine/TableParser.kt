package com.prslc.zhiflow.parser.engine

import androidx.compose.runtime.Stable
import com.prslc.zhiflow.data.model.Table
import com.prslc.zhiflow.parser.model.RichTextElement
import com.prslc.zhiflow.parser.model.ProcessedText

@Stable
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
            hasHeader = table.hasHeadRow
        )
    }
}