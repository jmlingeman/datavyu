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
package org.datavyu.util.FileFilters;

import javax.swing.filechooser.FileFilter;
import java.io.File;


/**
 * A file filter for legacy MacSHAPA database files.
 */
public class MobdFilter extends FileFilter {

    public static final MobdFilter INSTANCE = new MobdFilter();

    private MobdFilter() {
    }

    /**
     * @return The description of the file filter.
     */
    @Override
    public String getDescription() {
        return "MacSHAPA open database file (*.odb)";
    }

    /**
     * Determines if the file filter will accept the supplied file.
     *
     * @param file The file to check if this file will accept.
     * @return true if the file is to be accepted, false otherwise.
     */
    @Override
    public boolean accept(final File file) {
        return (file.getName().endsWith(".odb") || file.isDirectory());
    }
}
