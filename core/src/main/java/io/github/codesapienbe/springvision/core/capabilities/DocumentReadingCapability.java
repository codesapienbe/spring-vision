package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.document.DocumentContent;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability for reading a document (PDF, Word, Excel, PowerPoint) or an image into its text lines
 * plus what the file itself declares: tables, the filled-in fields of a PDF form, and its
 * properties. It assumes no layout: the same reading for any kind of document.
 */
public interface DocumentReadingCapability {

    /**
     * Reads a document or an image.
     *
     * @param imageData the document's or image's bytes
     * @return the text lines, tables, form fields and properties; for an image only the lines
     * @throws BaseVisionException if the document cannot be read
     */
    DocumentContent readDocument(ImageData imageData) throws BaseVisionException;
}
