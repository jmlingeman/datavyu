/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.datavyu.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.models.db.Cell;
import org.datavyu.views.discrete.SpreadSheetPanel;

import java.util.List;

/**
 * Controller for deleting cells from the database.
 */
public final class DeleteCellController {

    /** The logger for this class. */
    private static final Logger logger = LogManager.getLogger(DeleteCellController.class);

    /**
     * Constructor.
     *
     * @param cellsToDelete Cells to delete from the spreadsheet.
     */
    public DeleteCellController(final List<Cell> cellsToDelete) {
        logger.info("Delete cells: " + cellsToDelete);

        // The spreadsheet is the view for this controller.
        SpreadSheetPanel view = (SpreadSheetPanel) Datavyu.getView().getComponent();
        view.deselectAll();

        for (Cell cell : cellsToDelete) {
            // Check if the cell we are deleting is the last created cell. Default this back to 0 if it is.
            if (cell.equals(Datavyu.getProjectController().getLastCreatedCell())) {
                Datavyu.getProjectController().setLastCreatedCell(null);
            }
            // Check if the cell we are deleting is the last selected cell. Default this back to 0 if it is.
            if (cell.equals(Datavyu.getProjectController().getLastSelectedCell())) {
                Datavyu.getProjectController().setLastSelectedCell(null);
            }
            Datavyu.getProjectController().getDataStore().removeCell(cell);
        }
    }
}
