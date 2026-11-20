package io.github.codesapienbe.springvision.core.djl;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import io.github.codesapienbe.springvision.core.document.DocumentContent;

/**
 * Collects the tables from the XHTML that Apache Tika writes for a document: every {@code <table>}
 * becomes rows of cell texts. A table inside a cell is read as part of that cell's text. Rows
 * without any text are left out, and so is a table that ends up with no rows.
 *
 * <p>A SAX handler is filled by the parser through its callbacks; the shape is imposed by SAX.</p>
 */
final class TableCollector extends DefaultHandler {

    private final List<DocumentContent.Table> tables = new ArrayList<>();
    private List<List<String>> rows;
    private List<String> row;
    private StringBuilder cell;
    private int depth;

    /** The tables collected so far. */
    List<DocumentContent.Table> tables() {
        return List.copyOf(tables);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        switch (localName) {
            case "table" -> {
                depth++;
                if (depth == 1) {
                    rows = new ArrayList<>();
                }
            }
            case "tr" -> {
                if (depth == 1) {
                    row = new ArrayList<>();
                }
            }
            case "td", "th" -> {
                if (depth == 1) {
                    cell = new StringBuilder();
                }
            }
            case "p", "br", "div" -> separateWithin();
            default -> {
                // other elements carry no table structure
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        switch (localName) {
            case "table" -> {
                if (depth == 1 && rows != null && !rows.isEmpty()) {
                    tables.add(new DocumentContent.Table(null, rows));
                }
                depth = Math.max(0, depth - 1);
            }
            case "tr" -> {
                if (depth == 1 && row != null && row.stream().anyMatch(text -> !text.isEmpty())) {
                    rows.add(row);
                }
                if (depth == 1) {
                    row = null;
                }
            }
            case "td", "th" -> {
                if (depth == 1 && cell != null && row != null) {
                    row.add(cell.toString().replaceAll("\\s+", " ").strip());
                    cell = null;
                }
            }
            default -> {
                // other elements carry no table structure
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (cell != null) {
            cell.append(ch, start, length);
        }
    }

    /** A paragraph or line break inside a cell becomes a space, so its words don't run together. */
    private void separateWithin() {
        if (cell != null && !cell.isEmpty()) {
            cell.append(' ');
        }
    }
}
