package org.openshapa.controllers;

import org.openshapa.OpenSHAPA;
import org.openshapa.db.DataCell;
import org.openshapa.db.DataColumn;
import org.openshapa.db.Database;
import org.openshapa.db.MatrixVocabElement;
import org.openshapa.db.SystemErrorException;
import org.openshapa.db.TimeStamp;
import org.openshapa.util.Constants;
import org.openshapa.views.discrete.SpreadsheetPanel;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Controller for creating new cell.
 */
public final class CreateNewCellC {

    /**
     * Default constructor.
     */
    public CreateNewCellC() {
        // The spreadsheet is the view for this controller.
        view = (SpreadsheetPanel) OpenSHAPA.getApplication()
                                           .getMainView()
                                           .getComponent();
        model = OpenSHAPA.getDatabase();

        this.createNewCell(-1);
    }

    /**
     * Constructor - creates new controller.
     *
     * @param milliseconds The milliseconds to use for the onset for the new
     * cell.
     */
    public CreateNewCellC(final long milliseconds) {
        // The spreadsheet is the view for this controller.
        view = (SpreadsheetPanel) OpenSHAPA.getApplication()
                                           .getMainView()
                                           .getComponent();
        model = OpenSHAPA.getDatabase();

        this.createNewCell(milliseconds);
    }

    /**
     * Create a new cell with given onset. Currently just appends to the
     * selected column or the column that last had a cell added to it.
     *
     * @param milliseconds The number of milliseconds since the origin of the
     * spreadsheet to create a new cell from.
     */
    public void createNewCell(final long milliseconds) {
        /*
         * Concept of operation: Creating a new cell.
         *
         * Situation 1: Spreadsheet has one or more selected columns
         *  For each selected column do
         *      Create a new cell with the supplied onset and insert into db.
         *
         * Situation 2: Spreadsheet has one or more selected cells
         *  For each selected cell do
         *      Create a new cell with the selected cell onset and offset and
         *      insert into the db.
         *
         * Situation 3: User has set focus on a particular cell in the
         *      spreadsheet - the caret is or has been in one of the editable
         *      parts of a spreadsheet cell.
         *  First check this request has not come from the video controller.
         *  For the focussed cell do
         *      Create a new cell with the focussed cell onset and offset and
         *      insert into the db.
         *
         * Situation 4: Request has come from the video controller and there
         *      is no currently selected column.
         *  Create a new cell in the same column as the last created cell or
         *  the last focussed cell.
         */
        try {
            long onset = milliseconds;
            // if not coming from video controller (milliseconds < 0) allow
            // multiple adds
            boolean multiadd = (milliseconds < 0);
            if (milliseconds < 0) {
                onset = 0;
            }

            boolean newcelladded = false;
            // check for Situation 1: one or more selected columns
            for (DataColumn col : view.getSelectedCols()) {
                MatrixVocabElement mve = model.getMatrixVE(col.getItsMveID());
                DataCell cell = new DataCell(col.getDB(),
                                             col.getID(),
                                             mve.getID());
                cell.setOnset(new TimeStamp(Constants.TICKS_PER_SECOND, onset));

                if (onset > 0) {
                    OpenSHAPA.setLastCreatedCellId(model.appendCell(cell));
                } else {
                    OpenSHAPA.setLastCreatedCellId(model.insertdCell(cell, 1));
                }
                OpenSHAPA.setLastCreatedColId(col.getID());

                newcelladded = true;
                if (!multiadd) {
                    break;
                }
            }

            if (!newcelladded) {
                // else check for Situation 2: one or more selected cells
                Iterator<DataCell> itCells = view.getSelectedCells().iterator();

                while (itCells.hasNext()) {
                    // reget the selected cell from the database using its id
                    // in case a previous insert has changed its ordinal.
                    // recasting to DataCell without checking as the iterator
                    // only returns DataCells (no ref cells allowed so far)
                    DataCell dc = (DataCell) model
                                             .getCell(itCells.next().getID());
                    DataCell cell = new DataCell(model,
                                                 dc.getItsColID(),
                                                 dc.getItsMveID());
                    if (multiadd) {
                        cell.setOnset(dc.getOnset());
                        cell.setOffset(dc.getOffset());
                        OpenSHAPA.setLastCreatedCellId(model
                                           .insertdCell(cell, dc.getOrd() + 1));
                    } else {
                        cell.setOnset(new TimeStamp(Constants.TICKS_PER_SECOND,
                                                    onset));
                        OpenSHAPA.setLastCreatedCellId(model.appendCell(cell));
                    }
                    OpenSHAPA.setLastCreatedColId(cell.getItsColID());
                    newcelladded = true;
                    if (!multiadd) {
                        break;
                    }
                }
            }

            if (!newcelladded && multiadd) {
                // else check for Situation 3: User is or was editing an
                // existing cell and has requested a new cell
                if (OpenSHAPA.getLastCreatedCellId() != 0) {
                    DataCell dc = (DataCell) model
                                     .getCell(OpenSHAPA.getLastCreatedCellId());
                    DataCell cell = new DataCell(model,
                                                 dc.getItsColID(),
                                                 dc.getItsMveID());
                    cell.setOnset(dc.getOnset());
                    cell.setOffset(dc.getOffset());
                    OpenSHAPA.setLastCreatedCellId(model
                                           .insertdCell(cell, dc.getOrd() + 1));
                    OpenSHAPA.setLastCreatedColId(cell.getItsColID());
                    newcelladded = true;
                }
            }

            if (!newcelladded) {
                // else go with Situation 4: Video controller requested
                // - create in the same column as the last created cell or
                // the last focussed cell.
                if (OpenSHAPA.getLastCreatedColId() == 0) {
                    OpenSHAPA.setLastCreatedColId(model.getDataColumns()
                                                       .get(0)
                                                       .getID());
                }

                // would throw by now if no columns exist
                DataColumn col = model.getDataColumn(OpenSHAPA
                                                        .getLastCreatedColId());

                DataCell cell = new DataCell(col.getDB(),
                                             col.getID(),
                                             col.getItsMveID());
                cell.setOnset(new TimeStamp(Constants.TICKS_PER_SECOND, onset));
                OpenSHAPA.setLastCreatedCellId(model.appendCell(cell));
            }
            view.deselectAll();
        } catch (SystemErrorException e) {
            logger.error("Unable to create a new cell.", e);
        }
    }

    /** The logger for this class. */
    private static Logger logger = Logger.getLogger(CreateNewCellC.class);

    /** The view (the spreadsheet) for this controller. */
    private SpreadsheetPanel view;

    /** The model (the database) for this controller. */
    private Database model;
}
