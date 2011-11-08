/*
 * $Id: MultiColumnText.java 4784 2011-03-15 08:33:00Z blowagie $
 *
 * This file is part of the iText (R) project.
 * Copyright (c) 1998-2011 1T3XT BVBA
 * Authors: Bruno Lowagie, Paulo Soares, et al.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY 1T3XT,
 * 1T3XT DISCLAIMS THE WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA, 02110-1301 USA, or download the license from the following URL:
 * http://itextpdf.com/terms-of-use/
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every PDF that is created
 * or manipulated using iText.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the iText software without
 * disclosing the source code of your own applications.
 * These activities include: offering paid services to customers as an ASP,
 * serving PDFs on the fly in a web application, shipping iText with a closed
 * source product.
 *
 * For more information, please contact iText Software Corp. at this
 * address: sales@itextpdf.com
 */
package com.itextpdf.text.pdf;

import java.util.ArrayList;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.ElementListener;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.error_messages.MessageLocalization;

/**
 * Formats content into one or more columns bounded by a
 * rectangle.  The columns may be simple rectangles or
 * more complicated shapes. Add all of the columns before
 * adding content. Column continuation is supported. A MultiColumnText object may be added to
 * a document using <CODE>Document.add</CODE>.
 * @author Steve Appling
 */
public class MultiColumnText implements Element {

    /** special constant for automatic calculation of height */
    public static final float AUTOMATIC = -1f;

    /**
     * total desiredHeight of columns.  If <CODE>AUTOMATIC</CODE>, this means fill pages until done.
     * This may be larger than one page
     */
    private float desiredHeight;

    /**
     * total height of element written out so far
     */
    private float totalHeight;

    /**
     * true if all the text could not be written out due to height restriction
     */
    private boolean overflow;

    /**
     * Top of the columns - y position on starting page.
     * If <CODE>AUTOMATIC</CODE>, it means current y position when added to document
     */
    private float top;

    /**
     * ColumnText object used to do all the real work.  This same object is used for all columns
     */
    private ColumnText columnText;

    /**
     * Array of <CODE>ColumnDef</CODE> objects used to define the columns
     */
    private ArrayList<ColumnDef> columnDefs;

    /**
     * true if all columns are simple (rectangular)
     */
    private boolean simple = true;

    private int currentColumn = 0;

    private float nextY = AUTOMATIC;

    private boolean columnsRightToLeft = false;

    private PdfDocument document;
    /**
     * Default constructor.  Sets height to <CODE>AUTOMATIC</CODE>.
     * Columns will repeat on each page as necessary to accommodate content length.
     */
    public MultiColumnText() {
        this(AUTOMATIC);
    }

    /**
     * Construct a MultiColumnText container of the specified height.
     * If height is <CODE>AUTOMATIC</CODE>, fill complete pages until done.
     * If a specific height is used, it may span one or more pages.
     *
     * @param height
     */
    public MultiColumnText(float height) {
        columnDefs = new ArrayList<ColumnDef>();
        desiredHeight = height;
        top = AUTOMATIC;
        // canvas will be set later
        columnText = new ColumnText(null);
        totalHeight = 0f;
    }

    /**
     * Construct a MultiColumnText container of the specified height
     * starting at the specified Y position.
     *
     * @param height
     * @param top
     */
    public MultiColumnText(float top, float height) {
        columnDefs = new ArrayList<ColumnDef>();
        desiredHeight = height;
        this.top = top;
        nextY = top;
        // canvas will be set later
        columnText = new ColumnText(null);
        totalHeight = 0f;
    }

    /**
     * Indicates that all of the text did not fit in the
     * specified height. Note that isOverflow will return
     * false before the MultiColumnText object has been
     * added to the document.  It will always be false if
     * the height is AUTOMATIC.
     *
     * @return true if there is still space left in the column
     */
    public boolean isOverflow() {
        return overflow;
    }

    /**
     * Copy the parameters from the specified ColumnText to use
     * when rendering.  Parameters like <CODE>setArabicOptions</CODE>
     * must be set in this way.
     *
     * @param sourceColumn
     */
    public void useColumnParams(ColumnText sourceColumn) {
        // note that canvas will be overwritten later
        columnText.setSimpleVars(sourceColumn);
    }

    /**
     * Add a new column.  The parameters are limits for each column
     * wall in the format of a sequence of points (x1,y1,x2,y2,...).
     *
     * @param left  limits for left column
     * @param right limits for right column
     */
    public void addColumn(float[] left, float[] right) {
        ColumnDef nextDef = new ColumnDef(left, right);
        if (!nextDef.isSimple()) simple = false;
        columnDefs.add(nextDef);
    }

    /**
     * Add a simple rectangular column with specified left
     * and right x position boundaries.
     *
     * @param left  left boundary
     * @param right right boundary
     */
    public void addSimpleColumn(float left, float right) {
        ColumnDef newCol = new ColumnDef(left, right);
        columnDefs.add(newCol);
    }

    /**
     * Add the specified number of evenly spaced rectangular columns.
     * Columns will be separated by the specified gutterWidth.
     *
     * @param left        left boundary of first column
     * @param right       right boundary of last column
     * @param gutterWidth width of gutter spacing between columns
     * @param numColumns  number of columns to add
     */
    public void addRegularColumns(float left, float right, float gutterWidth, int numColumns) {
        float currX = left;
        float width = right - left;
        float colWidth = (width - gutterWidth * (numColumns - 1)) / numColumns;
        for (int i = 0; i < numColumns; i++) {
            addSimpleColumn(currX, currX + colWidth);
            currX += colWidth + gutterWidth;
        }
    }

    /**
     * Adds a <CODE>Phrase</CODE> to the current text array.
     * Will not have any effect if addElement() was called before.
     * @param phrase the text
     * @since	2.1.5
     */
    public void addText(Phrase phrase) {
    	columnText.addText(phrase);
    }

    /**
     * Adds a <CODE>Chunk</CODE> to the current text array.
     * Will not have any effect if addElement() was called before.
     * @param chunk the text
     * @since	2.1.5
     */
    public void addText(Chunk chunk) {
    	columnText.addText(chunk);
    }

    /**
     * Add an element to be rendered in a column.
     * Note that you can only add a <CODE>Phrase</CODE>
     * or a <CODE>Chunk</CODE> if the columns are
     * not all simple.  This is an underlying restriction in
     * {@link com.itextpdf.text.pdf.ColumnText}
     *
     * @param element element to add
     * @throws DocumentException if element can't be added
     */
    public void addElement(Element element) throws DocumentException {
        if (simple) {
            columnText.addElement(element);
        } else if (element instanceof Phrase) {
            columnText.addText((Phrase) element);
        } else if (element instanceof Chunk) {
            columnText.addText((Chunk) element);
        } else {
            throw new DocumentException(MessageLocalization.getComposedMessage("can.t.add.1.to.multicolumntext.with.complex.columns", element.getClass()));
        }
    }


    /**
     * Write out the columns.  After writing, use
     * {@link #isOverflow()} to see if all text was written.
     * @param canvas PdfContentByte to write with
     * @param document document to write to (only used to get page limit info)
     * @param documentY starting y position to begin writing at
     * @return the current height (y position) after writing the columns
     * @throws DocumentException on error
     */
    public float write(PdfContentByte canvas, PdfDocument document, float documentY) throws DocumentException {
        this.document = document;
        columnText.setCanvas(canvas);
        if (columnDefs.isEmpty()) {
            throw new DocumentException(MessageLocalization.getComposedMessage("multicolumntext.has.no.columns"));
        }
        overflow = false;
        float currentHeight = 0;
        boolean done = false;
        try {
            while (!done) {
                if (top == AUTOMATIC) {
                    top = document.getVerticalPosition(true); // RS - 07/07/2005 - Get current doc writing position for top of columns on new page.
                }
                else if (nextY == AUTOMATIC) {
                    nextY = document.getVerticalPosition(true); // RS - 07/07/2005 - - Get current doc writing position for top of columns on new page.
                }
                ColumnDef currentDef = columnDefs.get(getCurrentColumn());
                columnText.setYLine(top);

                float[] left = currentDef.resolvePositions(Rectangle.LEFT);
                float[] right = currentDef.resolvePositions(Rectangle.RIGHT);
                if (document.isMarginMirroring() && document.getPageNumber() % 2 == 0){
                    float delta = document.rightMargin() - document.left();
                    left = left.clone();
                    right = right.clone();
                    for (int i = 0; i < left.length; i += 2) {
                        left[i] -= delta;
                    }
                    for (int i = 0; i < right.length; i += 2) {
                        right[i] -= delta;
                    }
                }

                currentHeight = Math.max(currentHeight, getHeight(left, right));

                if (currentDef.isSimple()) {
                    columnText.setSimpleColumn(left[2], left[3], right[0], right[1]);
                } else {
                    columnText.setColumns(left, right);
                }

                int result = columnText.go();
                if ((result & ColumnText.NO_MORE_TEXT) != 0) {
                    done = true;
                    top = columnText.getYLine();
                } else if (shiftCurrentColumn()) {
                    top = nextY;
                } else {  // check if we are done because of height
                    totalHeight += currentHeight;
                    if (desiredHeight != AUTOMATIC && totalHeight >= desiredHeight) {
                        overflow = true;
                        break;
                    } else {  // need to start new page and reset the columns
                        documentY = nextY;
                        newPage();
                        currentHeight = 0;
                    }
                }
            }
        } catch (DocumentException ex) {
            ex.printStackTrace();
            throw ex;
        }
        if (desiredHeight == AUTOMATIC && columnDefs.size() == 1) {
        	currentHeight = documentY - columnText.getYLine();
        }
        return currentHeight;
    }

    private void newPage() throws DocumentException {
        resetCurrentColumn();
        if (desiredHeight == AUTOMATIC) {
        	top = nextY = AUTOMATIC;
        }
        else {
        	top = nextY;
        }
        totalHeight = 0;
        if (document != null) {
            document.newPage();
        }
    }

    /**
     * Figure out the height of a column from the border extents
     *
     * @param left  left border
     * @param right right border
     * @return height
     */
    private float getHeight(float[] left, float[] right) {
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        for (int i = 0; i < left.length; i += 2) {
            min = Math.min(min, left[i + 1]);
            max = Math.max(max, left[i + 1]);
        }
        for (int i = 0; i < right.length; i += 2) {
            min = Math.min(min, right[i + 1]);
            max = Math.max(max, right[i + 1]);
        }
        return max - min;
    }


    /**
     * Processes the element by adding it to an
     * <CODE>ElementListener</CODE>.
     *
     * @param	listener	an <CODE>ElementListener</CODE>
     * @return	<CODE>true</CODE> if the element was processed successfully
     */
    public boolean process(ElementListener listener) {
        try {
            return listener.add(this);
        } catch (DocumentException de) {
            return false;
        }
    }

    /**
     * Gets the type of the text element.
     *
     * @return	a type
     */

    public int type() {
        return Element.MULTI_COLUMN_TEXT;
    }

    /**
     * Returns null - not used
     *
     * @return	null
     */

    public ArrayList<Chunk> getChunks() {
        return null;
    }

	/**
	 * @see com.itextpdf.text.Element#isContent()
	 * @since	iText 2.0.8
	 */
	public boolean isContent() {
		return true;
	}

	/**
	 * @see com.itextpdf.text.Element#isNestable()
	 * @since	iText 2.0.8
	 */
	public boolean isNestable() {
		return false;
	}

    /**
     * Calculates the appropriate y position for the bottom
     * of the columns on this page.
     *
     * @return the y position of the bottom of the columns
     */
    private float getColumnBottom() {
        if (desiredHeight == AUTOMATIC) {
            return document.bottom();
        } else {
            return Math.max(top - (desiredHeight - totalHeight), document.bottom());
        }
    }

    /**
     * Moves the text insertion point to the beginning of the next column, issuing a page break if
     * needed.
     * @throws DocumentException on error
     */
    public void nextColumn() throws DocumentException {
        currentColumn = (currentColumn + 1) % columnDefs.size();
        top = nextY;
        if (currentColumn == 0) {
            newPage();
        }
    }

    /**
     * Gets the current column.
     * @return the current column
     */
    public int getCurrentColumn() {
    	if (columnsRightToLeft) {
    		return columnDefs.size() - currentColumn - 1;
    	}
        return currentColumn;
    }

    /**
     * Resets the current column.
     */
    public void resetCurrentColumn() {
    	currentColumn = 0;
    }

    /**
     * Shifts the current column.
     * @return true if the current column has changed
     */
    public boolean shiftCurrentColumn() {
    	if (currentColumn + 1 < columnDefs.size()) {
            currentColumn++;
            return true;
    	}
    	return false;
    }

    /**
     * Sets the direction of the columns.
     * @param direction true = right2left; false = left2right
     */
    public void setColumnsRightToLeft(boolean direction) {
    	columnsRightToLeft = direction;
    }

    /** Sets the ratio between the extra word spacing and the extra character spacing
     * when the text is fully justified.
     * Extra word spacing will grow <CODE>spaceCharRatio</CODE> times more than extra character spacing.
     * If the ratio is <CODE>PdfWriter.NO_SPACE_CHAR_RATIO</CODE> then the extra character spacing
     * will be zero.
     * @param spaceCharRatio the ratio between the extra word spacing and the extra character spacing
     */
    public void setSpaceCharRatio(float spaceCharRatio) {
        columnText.setSpaceCharRatio(spaceCharRatio);
    }

    /** Sets the run direction.
     * @param runDirection the run direction
     */
    public void setRunDirection(int runDirection) {
        columnText.setRunDirection(runDirection);
    }

    /** Sets the arabic shaping options. The option can be AR_NOVOWEL,
     * AR_COMPOSEDTASHKEEL and AR_LIG.
     * @param arabicOptions the arabic shaping options
     */
    public void setArabicOptions(int arabicOptions) {
        columnText.setArabicOptions(arabicOptions);
    }

    /** Sets the default alignment
     * @param alignment the default alignment
     */
    public void setAlignment(int alignment) {
        columnText.setAlignment(alignment);
    }

    /**
     * Inner class used to define a column
     */
    private class ColumnDef {
        private float[] left;
        private float[] right;

        ColumnDef(float[] newLeft, float[] newRight) {
            left = newLeft;
            right = newRight;
        }

        ColumnDef(float leftPosition, float rightPosition) {
            left = new float[4];
            left[0] = leftPosition; // x1
            left[1] = top;          // y1
            left[2] = leftPosition; // x2
            if (desiredHeight == AUTOMATIC || top == AUTOMATIC) {
                left[3] = AUTOMATIC;
            } else {
                left[3] = top - desiredHeight;
            }

            right = new float[4];
            right[0] = rightPosition; // x1
            right[1] = top;           // y1
            right[2] = rightPosition; // x2
            if (desiredHeight == AUTOMATIC || top == AUTOMATIC) {
                right[3] = AUTOMATIC;
            } else {
                right[3] = top - desiredHeight;
            }
        }

        /**
         * Resolves the positions for the specified side of the column
         * into real numbers once the top of the column is known.
         *
         * @param side either <CODE>Rectangle.LEFT</CODE>
         *             or <CODE>Rectangle.RIGHT</CODE>
         * @return the array of floats for the side
         */
        float[] resolvePositions(int side) {
            if (side == Rectangle.LEFT) {
                return resolvePositions(left);
            } else {
                return resolvePositions(right);
            }
        }

        private float[] resolvePositions(float[] positions) {
            if (!isSimple()) {
                positions[1] = top;
                return positions;
            }
            if (top == AUTOMATIC) {
                // this is bad - must be programmer error
                throw new RuntimeException("resolvePositions called with top=AUTOMATIC (-1).  " +
                        "Top position must be set befure lines can be resolved");
            }
            positions[1] = top;
            positions[3] = getColumnBottom();
            return positions;
        }

        /**
         * Checks if column definition is a simple rectangle
         * @return true if it is a simple column
         */
        private boolean isSimple() {
            return left.length == 4 && right.length == 4 && left[0] == left[2] && right[0] == right[2];
        }

    }
}
