package org.openshapa.uitests;

import java.awt.Frame;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.fest.reflect.core.Reflection.method;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.FilenameFilter;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.filechooser.FileFilter;

import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.KeyPressInfo;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.DataControllerFixture;
import org.fest.swing.fixture.DialogFixture;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JOptionPaneFixture;
import org.fest.swing.fixture.JPanelFixture;
import org.fest.swing.fixture.JPopupMenuFixture;
import org.fest.swing.fixture.JSliderFixture;
import org.fest.swing.fixture.NeedleFixture;
import org.fest.swing.fixture.RegionFixture;
import org.fest.swing.fixture.SpreadsheetCellFixture;
import org.fest.swing.fixture.SpreadsheetPanelFixture;
import org.fest.swing.fixture.TimescaleFixture;
import org.fest.swing.fixture.TrackFixture;
import org.fest.swing.fixture.TracksEditorFixture;
import org.fest.swing.timing.Timeout;
import org.fest.swing.util.Platform;

import org.openshapa.OpenSHAPA;

import org.openshapa.models.db.SystemErrorException;

import org.openshapa.util.UIUtils;

import org.openshapa.views.DataControllerV;
import org.openshapa.views.OpenSHAPAFileChooser;
import org.openshapa.views.continuous.PluginManager;
import org.openshapa.views.discrete.SpreadsheetPanel;

import org.openshapa.models.db.TimeStamp;

import org.openshapa.util.FileFilters.OPFFilter;

import org.openshapa.views.NewProjectV;

import org.testng.Assert;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * Test for the Track View in the Data Controller.
 */
public final class UITrackViewerTest extends OpenSHAPATestClass {

    /**
     * Deleting these temp files before and after tests because Java does
     * not always delete them during the test case. Doing the deletes here
     * has resulted in consistent behaviour.
     */
    @AfterMethod @BeforeMethod protected void deleteFiles() {
        final String tempFolder = System.getProperty("java.io.tmpdir");

        // Delete temporary CSV and SHAPA files
        FilenameFilter ff = new FilenameFilter() {
                public boolean accept(final File dir, final String name) {
                    return (name.endsWith(".csv") || name.endsWith(".shapa")
                            || name.endsWith(".opf"));
                }
            };

        File tempDirectory = new File(tempFolder);
        String[] files = tempDirectory.list(ff);

        for (int i = 0; i < files.length; i++) {
            File file = new File(tempFolder + "/" + files[i]);
            file.deleteOnExit();
            file.delete();
        }
    }

    /**
    * Test needle movement to ensure needle time is the same as the clock time.
    */
    @Test public void testNeedleMovement() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        // 4. Move needle to 6 seconds on data controller time.
        boolean lessThan6seconds = true;

        while (lessThan6seconds) {
            TimeStamp currTS;

            try {
                dcf.getTrackMixerController().getNeedle().drag(1);
                currTS = new TimeStamp(dcf.getCurrentTime());
                lessThan6seconds = currTS.lt(new TimeStamp("00:00:06:000"));
            } catch (SystemErrorException ex) {
                Logger.getLogger(UITrackViewerTest.class.getName()).log(
                    Level.SEVERE, null, ex);
            }
        }

        Assert.assertEquals(dcf.getCurrentTime(),
            dcf.getTrackMixerController().getNeedle()
                .getCurrentTimeAsTimeStamp());
    }

    /**
     * Test needle movement to ensure needle can't go beyond start or end.
     */
    @Test public void testRangeOfNeedleMovement() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        // 4. Move needle beyond end time
        NeedleFixture needle = dcf.getTrackMixerController().getNeedle();
        int widthOfTrack = dcf.getTrackMixerController().getTracksEditor()
            .getTrack(0).getWidthInPixels();

        while (needle.getCurrentTimeAsLong() <= 0) {
            needle.drag(widthOfTrack);
        }

        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(), "00:01:00:000");

        // 5. Move needle beyond start time
        needle.drag(-1 * widthOfTrack);
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(), "00:00:00:000");
    }

    /**
     * Test needle movement by doubel clicking on timescale.
     */
    @Test public void testNeedleMovementByDoubleClick() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        // Double click 1/3, 1/2, 3/4 way of timescale
        NeedleFixture needle = dcf.getTrackMixerController().getNeedle();
        TimescaleFixture tf = dcf.getTrackMixerController().getTimescale();
        int third = tf.getWidth() / 3;
        int half = tf.getWidth() / 2;
        int threefourths = tf.getWidth() / 4 * 3;

        tf.doubleClickAt(third);
        Assert.assertTrue((needle.getCurrentTimeAsTimeStamp().startsWith(
                    "00:00:21"))
            || (needle.getCurrentTimeAsTimeStamp().startsWith("00:00:20"))
            || (needle.getCurrentTimeAsTimeStamp().startsWith("00:00:19")));
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            dcf.getCurrentTime());

        tf.doubleClickAt(half);
        Assert.assertTrue((needle.getCurrentTimeAsTimeStamp().startsWith(
                    "00:00:31"))
            || (needle.getCurrentTimeAsTimeStamp().startsWith("00:00:30"))
            || (needle.getCurrentTimeAsTimeStamp().startsWith("00:00:29")));
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            dcf.getCurrentTime());

        tf.doubleClickAt(threefourths);
        Assert.assertTrue((needle.getCurrentTimeAsTimeStamp().startsWith(
                    "00:00:46"))
            || (needle.getCurrentTimeAsTimeStamp().startsWith("00:00:45"))
            || (needle.getCurrentTimeAsTimeStamp().startsWith("00:00:44")));
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            dcf.getCurrentTime());
    }

    /**
    * Test needle movement by doubel clicking on timescale.
    */
    @Test public void testNeedleMovementByDoubleClickWithZoom() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        JSliderFixture zoomSlider = dcf.getTrackMixerController()
            .getZoomSlider();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        // Zoom in fully
        zoomSlider.slideToMaximum();

        // Double click 1/3, 1/2, 3/4 way of timescale
        NeedleFixture needle = dcf.getTrackMixerController().getNeedle();
        TimescaleFixture tf = dcf.getTrackMixerController().getTimescale();
        int third = tf.getWidth() / 3;
        int half = tf.getWidth() / 2;
        int threefourths = tf.getWidth() / 4 * 3;

        tf.doubleClickAt(third);
        Assert.assertTrue((needle.getCurrentTimeAsTimeStamp().startsWith(
                    "00:00:00:6"))
            || (needle.getCurrentTimeAsTimeStamp().startsWith("00:00:00:5")));
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            dcf.getCurrentTime());

        tf.doubleClickAt(half);
        Assert.assertTrue((needle.getCurrentTimeAsTimeStamp().startsWith(
                    "00:00:00:9"))
            || (needle.getCurrentTimeAsTimeStamp().startsWith("00:00:00:8")));
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            dcf.getCurrentTime());

        tf.doubleClickAt(threefourths);
        Assert.assertTrue((needle.getCurrentTimeAsTimeStamp().startsWith(
                    "00:00:01:4"))
            || (needle.getCurrentTimeAsTimeStamp().startsWith("00:00:01:3")));
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            dcf.getCurrentTime());
    }

    /**
    * Test needle movement by doubel clicking on timescale.
    */
    @Test public void testNeedleMovementByDoubleClickOutsideRegion() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        // Create new variable and new cell
        UIUtils.createNewVariable(mainFrameFixture, "v",
            UIUtils.VAR_TYPES[(int) (Math.random() * UIUtils.VAR_TYPES.length)]);
        ssPanel.column(0).click();
        dcf.pressCreateNewCellButton();

        SpreadsheetCellFixture cell = ssPanel.column(0).cell(1);

        // Create an onset and offset region using cell
        cell.onsetTimestamp().enterText("00:00:25:000");
        cell.offsetTimestamp().enterText("00:00:35:000");

        // Select cell
        cell.fillSelectCell(true);

        // Press region snap button
        dcf.getTrackMixerController().getSnapRegionButton().click();

        // Double click 1/3, 1/2, 3/4 way of timescale
        NeedleFixture needle = dcf.getTrackMixerController().getNeedle();
        TimescaleFixture tf = dcf.getTrackMixerController().getTimescale();
        int third = tf.getWidth() / 3;
        int half = tf.getWidth() / 2;
        int threefourths = tf.getWidth() / 4 * 3;

        tf.doubleClickAt(third);
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(), "00:00:25:000");
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            dcf.getCurrentTime());

        tf.doubleClickAt(half);
        Assert.assertTrue((needle.getCurrentTimeAsTimeStamp().startsWith(
                    "00:00:31"))
            || (needle.getCurrentTimeAsTimeStamp().startsWith("00:00:30"))
            || (needle.getCurrentTimeAsTimeStamp().startsWith("00:00:29")));
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            dcf.getCurrentTime());

        tf.doubleClickAt(threefourths);
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(), "00:00:35:000");
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            dcf.getCurrentTime());
    }

    /**
     * Test region movement and effect on needle.
     * The following cases are tested:
     * 1. Right region beyond start + needle moves with it
     * 2. Right region beyond end
     * 3. Left region beyond start
     * 4. Left region beyond end + needle moves with it
     * 5. Right region to middle + needle moves with it
     * 6. Left region can't cross (go beyond) right
     * 7. Right region can't cross left
     */
    @Test public void testRegionMovement() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        RegionFixture region = dcf.getTrackMixerController().getRegion();
        NeedleFixture needle = dcf.getTrackMixerController().getNeedle();
        int widthOfTrack = dcf.getTrackMixerController().getTracksEditor()
            .getTrack(0).getWidthInPixels();

        // TEST1. Right region beyond start + needle stays same
        while (region.getEndTimeAsLong() > 0) {
            region.dragEndMarker(-1 * widthOfTrack);
        }

        Assert.assertEquals(region.getEndTimeAsTimeStamp(), "00:00:00:000");
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(), "00:00:00:000");

        // TEST2. Right region beyond end + needle stays same
        while (region.getEndTimeAsLong() <= 0) {
            region.dragEndMarker(widthOfTrack);
        }

        Assert.assertEquals(region.getEndTimeAsTimeStamp(), "00:01:00:000");
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(), "00:00:00:000");

        // TEST3. Left region beyond end + needle moves with it
        while (region.getStartTimeAsLong() <= 0) {
            region.dragStartMarker(widthOfTrack);
        }

        Assert.assertEquals(region.getStartTimeAsTimeStamp(), "00:01:00:000");
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(), "00:01:00:000");

        // TEST4. Left region beyond start + needle stays same
        while (region.getStartTimeAsLong() > 0) {
            region.dragStartMarker(-1 * widthOfTrack);
        }

        Assert.assertEquals(region.getStartTimeAsTimeStamp(), "00:00:00:000");
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(), "00:01:00:000");

        // TEST5. Right region to middle + needle moves with it
        region.dragEndMarker(-1 * widthOfTrack / 4);

        TimeStamp endTS = null;

        try {
            endTS = new TimeStamp(region.getEndTimeAsTimeStamp());
            Assert.assertTrue((endTS.ge(new TimeStamp("00:00:30:000")))
                && (endTS.le(new TimeStamp("00:00:50:000"))));
        } catch (SystemErrorException ex) {
            Logger.getLogger(UITrackViewerTest.class.getName()).log(
                Level.SEVERE, null, ex);
        }

        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            endTS.toHMSFString());

        // TEST6. Left region can't cross (go beyond) right
        region.dragStartMarker(widthOfTrack);
        Assert.assertEquals(region.getStartTimeAsTimeStamp(),
            endTS.toHMSFString());
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            endTS.toHMSFString());

        region.dragStartMarker(-1 * widthOfTrack / 2);

        TimeStamp startTS = null;

        try {
            startTS = new TimeStamp(region.getStartTimeAsTimeStamp());
            Assert.assertTrue((startTS.ge(new TimeStamp("00:00:00:000")))
                && (startTS.le(new TimeStamp("00:00:40:000"))));
        } catch (SystemErrorException ex) {
            Logger.getLogger(UITrackViewerTest.class.getName()).log(
                Level.SEVERE, null, ex);
        }

        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            endTS.toHMSFString());

        // TEST7. Right region can't cross left
        region.dragEndMarker(-1 * widthOfTrack);
        Assert.assertEquals(region.getEndTimeAsTimeStamp(),
            startTS.toHMSFString());
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
            startTS.toHMSFString());
    }

    /**
     * Test moving track while locked and unlocked.
     */
    @Test public void testLockUnlockTrack() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        // 4. Drag track
        TrackFixture track = dcf.getTrackMixerController().getTracksEditor()
            .getTrack(0);
        Assert.assertEquals(track.getOffsetTimeAsLong(), 0);

        while (track.getOffsetTimeAsLong() <= 0) {
            track.drag(150);
        }

        long offset = track.getOffsetTimeAsLong();
        Assert.assertTrue(offset > 0, "offset=" + offset);

        // 5. Lock track
        track.pressLockButton();

        // 6. Try to drag track, shouldn't be able to.
        track.drag(150);
        Assert.assertEquals(track.getOffsetTimeAsLong(), offset);
        track.drag(-100);
        Assert.assertEquals(track.getOffsetTimeAsLong(), offset);
    }

    /**
     * Test snapping tracks.
     */
    @Test public void testTrackSnapping() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open first video
        String root = System.getProperty("testPath");
        final File videoFile1 = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile1.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile1);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            boolean worked = false;
            JFileChooserFixture jfcf = null;

            do {
                dcf.button("addDataButton").click();

                try {
                    jfcf = dcf.fileChooser();
                    jfcf.selectFile(videoFile1).approve();
                    worked = true;
                } catch (Exception e) {
                    // keep trying
                }
            } while (worked == false);
        }

        // 2. Get first window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid1 = ((Frame) it.next());
        FrameFixture vidWindow1 = new FrameFixture(mainFrameFixture.robot,
                vid1);

        vidWindow1.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        // c. Open second video
        final File videoFile2 = new File(root + "/ui/head_turns_copy.mov");
        Assert.assertTrue(videoFile2.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile2);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            boolean worked = false;
            JFileChooserFixture jfcf = null;

            do {
                dcf.button("addDataButton").click();

                try {
                    jfcf = dcf.fileChooser();
                    jfcf.selectFile(videoFile2).approve();
                    worked = true;
                } catch (Exception e) {
                    // keep trying
                }
            } while (worked == false);
        }

        // 2. Get second window
        it = dcf.getDataViewers().iterator();

        Frame vid2 = ((Frame) it.next());
        FrameFixture vidWindow2 = new FrameFixture(mainFrameFixture.robot,
                vid2);

        vidWindow2.moveTo(new Point(0, dcf.component().getHeight() + 130));

        // 3. Move needle 50 pixels
        NeedleFixture needle = dcf.getTrackMixerController().getNeedle();

        while (needle.getCurrentTimeAsLong() <= 0) {
            needle.drag(100);
        }

        long snapPoint1 = needle.getCurrentTimeAsLong();

        Assert.assertTrue(snapPoint1 > 0);

        // 4. Add bookmark to Track 1 using button
        // a. Click track 1 to select
        TrackFixture track1 = dcf.getTrackMixerController().getTracksEditor()
            .getTrack(0);

        while (!track1.isSelected()) {
            track1.click();
        }

        Assert.assertTrue(track1.isSelected());

        // b. Click add bookmark button
        dcf.getTrackMixerController().pressBookmarkButton();
        Assert.assertEquals(snapPoint1, track1.getBookmarkTimeAsLong());

        // c. Click track 1 to deselect
        while (track1.isSelected()) {
            track1.click();
        }

        Assert.assertFalse(track1.isSelected());

        // 5. Move needle another 50 pixels
        while (needle.getCurrentTimeAsLong() <= (1.9 * snapPoint1)) {
            dcf.getTrackMixerController().getNeedle().drag(100);
        }

        Assert.assertTrue(needle.getCurrentTimeAsLong() > snapPoint1);

        long snapPoint2 = dcf.getTrackMixerController().getNeedle()
            .getCurrentTimeAsLong();
        Assert.assertTrue(snapPoint2 > (1.9 * snapPoint1));

        // 6. Add bookmark to Track 2 using right click popup menu
        TrackFixture track2 = dcf.getTrackMixerController().getTracksEditor()
            .getTrack(1);
        JPopupMenuFixture popup = track2.showPopUpMenu();
        popup.menuItemWithPath("Set bookmark").click();
        Assert.assertEquals(snapPoint2, track2.getBookmarkTimeAsLong());

        while (track2.isSelected()) {
            track2.click();
        }

        Assert.assertFalse(track2.isSelected());

        // Move needle away
        while (needle.getCurrentTimeAsLong() <= snapPoint2) {
            needle.drag(100);
        }

        Assert.assertTrue(needle.getCurrentTimeAsLong() > snapPoint2);

        // 7. Drag track 1 to snap
        // a. Drag 1 pixel to see what 1 pixel equals in time
        track1.drag(10);

        long tempTime = track1.getOffsetTimeAsLong();
        track1.drag(1);

        long onePixelTime = track1.getOffsetTimeAsLong() - tempTime;

        // b. Drag away from start marker
        track1.drag(10);

        // c. Drag 1 pixel at a time until it snaps

        long newTime = onePixelTime;
        long oldTime = 0;

        while ((!dcf.getTrackMixerController().getTracksEditor().getSnapMarker()
                    .isVisible())
                || (Math.abs((newTime - oldTime) - onePixelTime) < 2)) {

            // Check if we've snapped
            if (
                dcf.getTrackMixerController().getTracksEditor().getSnapMarker()
                    .isVisible() && (newTime > (10 * onePixelTime))) {
                System.err.println("Snapped while moving");
                System.err.println("New time=" + newTime);
                System.err.println("Old time=" + oldTime);
                System.err.println("onePixelTime=" + onePixelTime);

                break;
            }

            // Check we haven't gone too far
            if (newTime > snapPoint1) {
                track1.releaseLeftMouse();
                Assert.assertTrue(false, "passed snap point");
            }

            oldTime = track1.getOffsetTimeAsLong();
            track1.dragWithoutReleasing(1);
            newTime = track1.getOffsetTimeAsLong();
        }

        System.err.println("Snapped?");
        System.err.println("New time=" + newTime);
        System.err.println("Old time=" + oldTime);
        System.err.println("onePixelTime=" + onePixelTime);
        System.err.println("snapPoint2=" + snapPoint2);
        System.err.println("snapPoint1=" + snapPoint1);
        System.err.println("trackOffset=" + track1.getOffsetTimeAsLong());

        // d. Check if snapped
        Assert.assertEquals(track1.getOffsetTimeAsLong(),
            snapPoint2 - snapPoint1);
        Assert.assertTrue(dcf.getTrackMixerController().getTracksEditor()
            .getSnapMarker().isVisible());
        track1.releaseLeftMouse();

        // 8. Drag track 1 to snap from the other direction
        // a. Drag track away
        track1.drag(35);

        // b. Drag 1 pixel at a time until it snaps
        newTime = track1.getOffsetTimeAsLong();
        oldTime = newTime + onePixelTime;

        while ((!dcf.getTrackMixerController().getTracksEditor().getSnapMarker()
                    .isVisible())
                || ((Math.abs((oldTime - newTime) - onePixelTime)) < 2)) {

            // Check if we've snapped
            if (
                dcf.getTrackMixerController().getTracksEditor().getSnapMarker()
                    .isVisible() && (newTime > (10 * onePixelTime))) {
                break;
            }

            // Check we haven't gone too far
            if (newTime < 0) {
                track1.releaseLeftMouse();
                Assert.assertTrue(false, "passed snap point");
            }

            oldTime = track1.getOffsetTimeAsLong();
            track1.dragWithoutReleasing(-1);
            newTime = track1.getOffsetTimeAsLong();
        }

        System.err.print(newTime - oldTime + "," + onePixelTime);

        // b. Check if snapped
        Assert.assertEquals(track1.getOffsetTimeAsLong(),
            snapPoint2 - snapPoint1);
        Assert.assertTrue(dcf.getTrackMixerController().getTracksEditor()
            .getSnapMarker().isVisible());
        track2.releaseLeftMouse();
    }

    /**
     * Test for Unlock Lock with zooming.
     */
    @Test public void testLockUnlockTrackWithZoom() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        JSliderFixture zoomSlider = dcf.getTrackMixerController()
            .getZoomSlider();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        // 3. Zoom track
        zoomSlider.slideTo(500);

        // 4. Drag track
        TrackFixture track = dcf.getTrackMixerController().getTracksEditor()
            .getTrack(0);
        Assert.assertEquals(track.getOffsetTimeAsLong(), 0);

        while (track.getOffsetTimeAsLong() <= 0) {
            track.drag(150);
        }

        long offset = track.getOffsetTimeAsLong();
        Assert.assertTrue(offset > 0, "offset=" + offset);

        // 5. Lock track
        track.pressLockButton();

        // 6. Try to drag track, shouldn't be able to.
        track.drag(150);
        Assert.assertEquals(track.getOffsetTimeAsLong(), offset);
        track.drag(-100);
        Assert.assertEquals(track.getOffsetTimeAsLong(), offset);

        /* BugzID: 1734
         * zoomSlider.slideToMaximum();
         * dcf.getTrackMixerController().getHorizontalScrollBar()
         * .scrollToMaximum();
         *
         * // 6. Try to drag track, shouldn't be able to. track.drag(150);
         * Assert.assertEquals(track.getOffsetTimeAsLong(), offset);
         * track.drag(-100); Assert.assertEquals(track.getOffsetTimeAsLong(),
         * offset);
         */
    }

    /**
     * Test needle movement to ensure needle can't go beyond start or end,
     * with zoom applied.
     */
    @Test public void testRangeOfNeedleMovementWithZoom() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        JSliderFixture zoomSlider = dcf.getTrackMixerController()
            .getZoomSlider();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        zoomSlider.slideTo(200);

        // 4. Move needle beyond end time
        NeedleFixture needle = dcf.getTrackMixerController().getNeedle();
        int widthOfTrack = dcf.getTrackMixerController().getTracksEditor()
            .getTrack(0).getWidthInPixels();

        while (needle.getCurrentTimeAsLong() <= 0) {
            needle.drag(widthOfTrack);
        }

        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(), "00:00:30:000");

        // 5. Move needle beyond start time
        needle.drag(-1 * widthOfTrack);
        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(), "00:00:00:000");

        // Drag back to half way
        while (needle.getCurrentTimeAsLong() <= 0) {
            needle.drag(widthOfTrack);
        }

        Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(), "00:00:30:000");

        /*BugzID:1734
         * dcf.getTrackMixerController().getHorizontalScrollBar().scrollToMaximum();
         *
         * Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
         * "00:00:30:000");
         *
         * while (needle.getCurrentTimeAsLong() <= 0) { needle.drag(widthOfTrack);
         * }
         *
         * Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
         * "00:01:00:000");
         *
         * // 5. Move needle beyond start time needle.drag(-1 * widthOfTrack);
         * Assert.assertEquals(needle.getCurrentTimeAsTimeStamp(),
         * "00:00:30:000");
         */
    }

    /**
     * Test snapping tracks.
     */
    @Test public void testTrackSnappingWithZoom() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        JSliderFixture zoomSlider = dcf.getTrackMixerController()
            .getZoomSlider();

        // c. Open first video
        String root = System.getProperty("testPath");
        final File videoFile1 = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile1.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile1);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            boolean worked = false;
            JFileChooserFixture jfcf = null;

            do {
                dcf.button("addDataButton").click();

                try {
                    jfcf = dcf.fileChooser();
                    jfcf.selectFile(videoFile1).approve();
                    worked = true;
                } catch (Exception e) {
                    // keep trying
                }
            } while (worked == false);
        }

        // 2. Get first window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid1 = ((Frame) it.next());
        FrameFixture vidWindow1 = new FrameFixture(mainFrameFixture.robot,
                vid1);

        vidWindow1.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        // c. Open second video
        final File videoFile2 = new File(root + "/ui/head_turns_copy.mov");
        Assert.assertTrue(videoFile2.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile2);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            boolean worked = false;
            JFileChooserFixture jfcf = null;

            do {
                dcf.button("addDataButton").click();

                try {
                    jfcf = dcf.fileChooser();
                    jfcf.selectFile(videoFile2).approve();
                    worked = true;
                } catch (Exception e) {
                    // keep trying
                }
            } while (worked == false);
        }

        // 2. Get second window
        it = dcf.getDataViewers().iterator();

        Frame vid2 = ((Frame) it.next());
        FrameFixture vidWindow2 = new FrameFixture(mainFrameFixture.robot,
                vid2);

        vidWindow2.moveTo(new Point(0, dcf.component().getHeight() + 130));

        // Zoom
        zoomSlider.slideToMaximum();

        // 3. Move needle 50 pixels
        NeedleFixture needle = dcf.getTrackMixerController().getNeedle();

        while (needle.getCurrentTimeAsLong() <= 0) {
            needle.drag(100);
        }

        long snapPoint1 = needle.getCurrentTimeAsLong();

        Assert.assertTrue(snapPoint1 > 0);

        // 4. Add bookmark to Track 1 using button
        // a. Click track 1 to select
        TrackFixture track1 = dcf.getTrackMixerController().getTracksEditor()
            .getTrack(0);

        while (!track1.isSelected()) {
            track1.click();
        }

        Assert.assertTrue(track1.isSelected());

        // b. Click add bookmark button
        dcf.getTrackMixerController().pressBookmarkButton();
        Assert.assertEquals(snapPoint1, track1.getBookmarkTimeAsLong());

        // c. Click track 1 to deselect
        while (track1.isSelected()) {
            track1.click();
        }

        Assert.assertFalse(track1.isSelected());

        // 5. Move needle another 50 pixels
        while (needle.getCurrentTimeAsLong() <= (1.9 * snapPoint1)) {
            dcf.getTrackMixerController().getNeedle().drag(100);
        }

        Assert.assertTrue(needle.getCurrentTimeAsLong() > snapPoint1);

        long snapPoint2 = dcf.getTrackMixerController().getNeedle()
            .getCurrentTimeAsLong();
        Assert.assertTrue(snapPoint2 > (1.9 * snapPoint1));

        // 6. Add bookmark to Track 2 using right click popup menu
        TrackFixture track2 = dcf.getTrackMixerController().getTracksEditor()
            .getTrack(1);
        JPopupMenuFixture popup = track2.showPopUpMenu();
        popup.menuItemWithPath("Set bookmark").click();
        Assert.assertEquals(snapPoint2, track2.getBookmarkTimeAsLong());

        while (track2.isSelected()) {
            track2.click();
        }

        Assert.assertFalse(track2.isSelected());

        // Move needle away
        while (needle.getCurrentTimeAsLong() <= snapPoint2) {
            needle.drag(100);
        }

        Assert.assertTrue(needle.getCurrentTimeAsLong() > snapPoint2);

        // 7. Drag track 1 to snap
        // a. Drag 1 pixel to see what 1 pixel equals in time
        track1.drag(10);

        long tempTime = track1.getOffsetTimeAsLong();
        track1.drag(1);

        long onePixelTime = track1.getOffsetTimeAsLong() - tempTime;

        // b. Drag away from start marker
        track1.drag(10);

        // c. Drag 1 pixel at a time until it snaps

        long newTime = onePixelTime;
        long oldTime = 0;

        while ((!dcf.getTrackMixerController().getTracksEditor().getSnapMarker()
                    .isVisible())
                || (Math.abs((newTime - oldTime) - onePixelTime) < 2)) {

            // Check if we've snapped
            if (
                dcf.getTrackMixerController().getTracksEditor().getSnapMarker()
                    .isVisible() && (newTime > (10 * onePixelTime))) {
                System.err.println("Snapped while moving");

                break;
            }

            // Check we haven't gone too far
            if (newTime > snapPoint1) {
                track1.releaseLeftMouse();
                Assert.assertTrue(false, "passed snap point");
            }

            oldTime = track1.getOffsetTimeAsLong();
            track1.dragWithoutReleasing(1);
            newTime = track1.getOffsetTimeAsLong();
        }

        // d. Check if snapped
        Assert.assertEquals(track1.getOffsetTimeAsLong(),
            snapPoint2 - snapPoint1);
        Assert.assertTrue(dcf.getTrackMixerController().getTracksEditor()
            .getSnapMarker().isVisible());
        track1.releaseLeftMouse();

        // 8. Drag track 1 to snap from the other direction
        // a. Drag track away
        track1.drag(35);

        // b. Drag 1 pixel at a time until it snaps
        newTime = track1.getOffsetTimeAsLong();
        oldTime = newTime + onePixelTime;

        long startTime = newTime;


        while ((!dcf.getTrackMixerController().getTracksEditor().getSnapMarker()
                    .isVisible())
                || (Math.abs((oldTime - newTime) - onePixelTime) < 2)) {

            // Check if we've snapped
            if (
                dcf.getTrackMixerController().getTracksEditor().getSnapMarker()
                    .isVisible()
                    && (newTime < (startTime - (10 * onePixelTime)))) {
                break;
            }

            // Check we haven't gone too far
            if (newTime < 0) {
                track1.releaseLeftMouse();
                Assert.assertTrue(false, "passed snap point");
            }

            oldTime = track1.getOffsetTimeAsLong();
            track1.dragWithoutReleasing(-1);

            newTime = track1.getOffsetTimeAsLong();
        }

        System.err.print(newTime - oldTime + "," + onePixelTime);

        // b. Check if snapped
        Assert.assertEquals(track1.getOffsetTimeAsLong(),
            snapPoint2 - snapPoint1);
        Assert.assertTrue(dcf.getTrackMixerController().getTracksEditor()
            .getSnapMarker().isVisible());
        track2.releaseLeftMouse();
    }

    /**
    * Test closing of video while play.
    * Should reset datacontroller and remove track.
    */
    /*BugzID1796@Test*/ public void testCloseVideoWhilePlaying() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        vid.setAlwaysOnTop(true);

        File refImageFile = new File(root + "/ui/head_turns600h0t.png");
        vid.toFront();

        // 4. Play video for 3 seconds
        dcf.pressPlayButton();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(UITrackViewerTest.class.getName()).log(
                Level.SEVERE, null, ex);
        }

        // Assert that time is not 0.
        Assert.assertFalse(dcf.getCurrentTime().equals("00:00:00:000"));

        // Assert that track is present
        Assert.assertEquals(dcf.getTrackMixerController().getTracksEditor()
            .getTracks().size(), 1);

        // Close window
        vidWindow.close();

        // Check that everything is reset
        Assert.assertTrue(dcf.getCurrentTime().equals("00:00:00:000"));
        Assert.assertEquals(dcf.getTrackMixerController().getTracksEditor()
            .getTracks().size(), 0);
    }

    /**
    * Test closing of video.
    * Should reset datacontroller and remove track.
    */
    @Test public void testCloseVideo() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        vid.setAlwaysOnTop(true);

        File refImageFile = new File(root + "/ui/head_turns600h0t.png");
        vid.toFront();

        // 4. Play video for 3 seconds
        dcf.pressPlayButton();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(UITrackViewerTest.class.getName()).log(
                Level.SEVERE, null, ex);
        }

        dcf.pressStopButton();

        // Assert that time is not 0.
        Assert.assertFalse(dcf.getCurrentTime().equals("00:00:00:000"));

        // Assert that track is present
        Assert.assertEquals(dcf.getTrackMixerController().getTracksEditor()
            .getTracks().size(), 1);

        // Close window
        vidWindow.close();

        // Check that everything is reset
        Assert.assertTrue(dcf.getCurrentTime().equals("00:00:00:000"));
        Assert.assertEquals(dcf.getTrackMixerController().getTracksEditor()
            .getTracks().size(), 0);
    }

    /**
    * Test hiding and showing the video.
    */
    @Test public void testShowHideVideo() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        vid.setAlwaysOnTop(true);

        File refImageFile = new File(root + "/ui/head_turns600h0t.png");
        vid.toFront();

        // 4. Play video for 3 seconds
        dcf.pressPlayButton();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(UITrackViewerTest.class.getName()).log(
                Level.SEVERE, null, ex);
        }

        dcf.pressStopButton();

        // Assert that time is not 0.
        Assert.assertFalse(dcf.getCurrentTime().equals("00:00:00:000"));

        String currTime = dcf.getCurrentTime();

        // Assert that track is present
        Assert.assertEquals(dcf.getTrackMixerController().getTracksEditor()
            .getTracks().size(), 1);

        // Hide window
        dcf.getTrackMixerController().getTracksEditor().getTrack(0)
            .pressActionButton2();

        // Check that video is not visible, but track is present and time is
        // the same
        Assert.assertTrue(dcf.getCurrentTime().equals(currTime));
        Assert.assertEquals(dcf.getTrackMixerController().getTracksEditor()
            .getTracks().size(), 1);
        vidWindow.requireNotVisible();

        // Close window
        dcf.getTrackMixerController().getTracksEditor().getTrack(0)
            .pressActionButton2();

        // Check that video is  visible and track is present and time is the
        // same
        Assert.assertTrue(dcf.getCurrentTime().equals(currTime));
        Assert.assertEquals(dcf.getTrackMixerController().getTracksEditor()
            .getTracks().size(), 1);
        vidWindow.requireVisible();
    }

    /**
    * Test hiding and showing the video.
    */
    @Test public void testShowHideVideoWhilePlaying() throws Exception {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        vid.setAlwaysOnTop(true);

        File refImageFile = new File(root + "/ui/head_turns600h0t.png");
        vid.toFront();

        // 4. Play video
        dcf.pressPlayButton();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(UITrackViewerTest.class.getName()).log(
                Level.SEVERE, null, ex);
        }

        // Assert that time is not 0.
        Assert.assertFalse(dcf.getCurrentTime().equals("00:00:00:000"));

        String currTime = dcf.getCurrentTime();

        // Assert that track is present
        Assert.assertEquals(dcf.getTrackMixerController().getTracksEditor()
            .getTracks().size(), 1);

        // Hide window
        dcf.getTrackMixerController().getTracksEditor().getTrack(0)
            .pressActionButton2();

        // Check that video is not visible, but track is present playing
        Assert.assertTrue(new TimeStamp(dcf.getCurrentTime()).gt(
                new TimeStamp(currTime)));
        currTime = dcf.getCurrentTime();
        Assert.assertEquals(dcf.getTrackMixerController().getTracksEditor()
            .getTracks().size(), 1);
        vidWindow.requireNotVisible();

        // Close window
        dcf.getTrackMixerController().getTracksEditor().getTrack(0)
            .pressActionButton2();

        // Check that video is  visible and track is present and time is the
        // same
        Assert.assertTrue(new TimeStamp(dcf.getCurrentTime()).gt(
                new TimeStamp(currTime)));
        Assert.assertEquals(dcf.getTrackMixerController().getTracksEditor()
            .getTracks().size(), 1);
        vidWindow.requireVisible();
    }

    /**
    * Test needle movement to ensure needle time is the same as the clock time.
    */
    @Test public void testRegionSnapping() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open video
        String root = System.getProperty("testPath");
        final File videoFile = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            dcf.button("addDataButton").click();

            JFileChooserFixture jfcf = dcf.fileChooser();
            jfcf.selectFile(videoFile).approve();
        }

        // 2. Get window
        Iterator it = dcf.getDataViewers().iterator();

        Frame vid = ((Frame) it.next());
        FrameFixture vidWindow = new FrameFixture(mainFrameFixture.robot, vid);

        vidWindow.moveTo(new Point(dcf.component().getWidth() + 10, 100));

        // Create new variable and new cell
        UIUtils.createNewVariable(mainFrameFixture, "v",
            UIUtils.VAR_TYPES[(int) (Math.random() * UIUtils.VAR_TYPES.length)]);
        ssPanel.column(0).click();
        dcf.pressCreateNewCellButton();

        SpreadsheetCellFixture cell = ssPanel.column(0).cell(1);

        // Create an onset and offset region using cell
        cell.onsetTimestamp().enterText("00:00:20:000");
        cell.offsetTimestamp().enterText("00:00:40:000");

        // Select cell
        cell.fillSelectCell(true);

        // Press region snap button
        dcf.getTrackMixerController().getSnapRegionButton().click();

        // Check that region was snapped
        Assert.assertEquals(dcf.getTrackMixerController().getRegion()
            .getStartTimeAsTimeStamp(), "00:00:20:000");
        Assert.assertEquals(dcf.getTrackMixerController().getRegion()
            .getEndTimeAsTimeStamp(), "00:00:40:000");
    }

    /**
     * Test preservation of track order after save.
     */
    @Test public void testTrackOrderAfterSave() {
        System.err.println(new Exception().getStackTrace()[0].getMethodName());

        final String tempFolder = System.getProperty("java.io.tmpdir");

        // 1. Get Spreadsheet
        JPanelFixture jPanel = UIUtils.getSpreadsheet(mainFrameFixture);
        SpreadsheetPanelFixture ssPanel = new SpreadsheetPanelFixture(
                mainFrameFixture.robot, (SpreadsheetPanel) jPanel.component());

        // 2. Open Data Viewer Controller and get starting time
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        // 3. Open track view
        dcf.pressShowTracksButton();

        // c. Open videos
        String root = System.getProperty("testPath");
        final File videoFile1 = new File(root + "/ui/head_turns.mov");
        Assert.assertTrue(videoFile1.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile1);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            boolean worked = false;
            JFileChooserFixture jfcf = null;

            do {
                dcf.button("addDataButton").click();

                try {
                    jfcf = dcf.fileChooser();
                    jfcf.selectFile(videoFile1).approve();
                    worked = true;
                } catch (Exception e) {
                    // keep trying
                }
            } while (worked == false);
        }

        final File videoFile2 = new File(root + "/ui/head_turns_copy.mov");
        Assert.assertTrue(videoFile2.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile2);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            boolean worked = false;
            JFileChooserFixture jfcf = null;

            do {
                dcf.button("addDataButton").click();

                try {
                    jfcf = dcf.fileChooser();
                    jfcf.selectFile(videoFile2).approve();
                    worked = true;
                } catch (Exception e) {
                    // keep trying
                }
            } while (worked == false);
        }

        final File videoFile3 = new File(root + "/ui/head_turns2.mov");
        Assert.assertTrue(videoFile3.exists());

        if (Platform.isOSX()) {
            final PluginManager pm = PluginManager.getInstance();

            GuiActionRunner.execute(new GuiTask() {
                    public void executeInEDT() {
                        OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
                        fc.setVisible(false);

                        for (FileFilter f : pm.getPluginFileFilters()) {
                            fc.addChoosableFileFilter(f);
                        }

                        fc.setSelectedFile(videoFile3);
                        method("openVideo").withParameterTypes(
                            OpenSHAPAFileChooser.class).in(
                            (DataControllerV) dcf.component()).invoke(fc);
                    }
                });
        } else {
            boolean worked = false;
            JFileChooserFixture jfcf = null;

            do {
                dcf.button("addDataButton").click();

                try {
                    jfcf = dcf.fileChooser();
                    jfcf.selectFile(videoFile3).approve();
                    worked = true;
                } catch (Exception e) {
                    // keep trying
                }
            } while (worked == false);
        }

        // Get track order
        String[] tracksArray = new String[3];
        TracksEditorFixture tracks = dcf.getTrackMixerController()
            .getTracksEditor();

        for (int i = 0; i < tracks.getTracks().size(); i++) {
            tracksArray[i] = tracks.getTrack(i).getTrackName();
        }

        // Save project
        File tempFile = new File(tempFolder + "/testTrackOrderAfterSave.opf");

        if (Platform.isOSX()) {
            OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
            fc.setVisible(false);
            fc.setFileFilter(new OPFFilter());

            fc.setSelectedFile(tempFile);

            method("save").withParameterTypes(OpenSHAPAFileChooser.class).in(
                OpenSHAPA.getView()).invoke(fc);
        } else {
            mainFrameFixture.clickMenuItemWithPath("File", "Save As...");

            mainFrameFixture.fileChooser().component().setFileFilter(
                new OPFFilter());
            mainFrameFixture.fileChooser().selectFile(tempFile).approve();
        }

        // New project
        // 3. Create a new database (and discard unsaved changes)
        if (Platform.isOSX()) {
            mainFrameFixture.pressAndReleaseKey(KeyPressInfo.keyCode(
                    KeyEvent.VK_N).modifiers(InputEvent.META_MASK));
        } else {
            mainFrameFixture.clickMenuItemWithPath("File", "New");
        }

        DialogFixture newDatabaseDialog;

        try {
            newDatabaseDialog = mainFrameFixture.dialog();
        } catch (Exception e) {

            // Get New Database dialog
            newDatabaseDialog = mainFrameFixture.dialog(
                    new GenericTypeMatcher<JDialog>(JDialog.class) {
                        @Override protected boolean isMatching(
                            final JDialog dialog) {
                            return dialog.getClass().equals(NewProjectV.class);
                        }
                    }, Timeout.timeout(5, TimeUnit.SECONDS));
        }

        newDatabaseDialog.textBox("nameField").enterText("n");

        newDatabaseDialog.button("okButton").click();

        // 4a. Check that all videos are cleared
        Assert.assertEquals(tracks.getTracks().size(), 0);

        // Reopen project
        if (Platform.isOSX()) {
            OpenSHAPAFileChooser fc = new OpenSHAPAFileChooser();
            fc.setVisible(false);

            fc.setFileFilter(new OPFFilter());
            fc.setSelectedFile(tempFile);

            method("open").withParameterTypes(OpenSHAPAFileChooser.class).in(
                OpenSHAPA.getView()).invoke(fc);
        } else {
            mainFrameFixture.clickMenuItemWithPath("File", "Open...");

            try {
                JOptionPaneFixture warning = mainFrameFixture.optionPane();
                warning.requireTitle("Unsaved changes");
                warning.buttonWithText("OK").click();
            } catch (Exception e) {
                // Do nothing
            }

            mainFrameFixture.fileChooser().component().setFileFilter(
                new OPFFilter());

            mainFrameFixture.fileChooser().selectFile(tempFile).approve();
        }

        // Check track order
        mainFrameFixture.clickMenuItemWithPath("Controller",
            "Data Viewer Controller");
        mainFrameFixture.dialog().moveTo(new Point(0, 100));

        final DataControllerFixture dcf2 = new DataControllerFixture(
                mainFrameFixture.robot,
                (DataControllerV) mainFrameFixture.dialog().component());

        dcf2.pressShowTracksButton();

        TracksEditorFixture tracks2 = dcf2.getTrackMixerController()
            .getTracksEditor();

        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(tracksArray[i],
                tracks2.getTrack(i).getTrackName());
        }
    }
}
