package org.openshapa.controllers.project;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.openshapa.OpenSHAPA;
import org.openshapa.models.component.TrackModel;
import org.openshapa.models.db.MacshapaDatabase;
import org.openshapa.models.project.Project;
import org.openshapa.models.project.TrackSettings;
import org.openshapa.models.project.ViewerSetting;
import org.openshapa.views.DataControllerV;
import org.openshapa.views.MixerControllerV;
import org.openshapa.views.continuous.DataViewer;
import org.openshapa.views.continuous.Plugin;
import org.openshapa.views.continuous.PluginManager;

/**
 * This class is responsible for managing a project.
 * 
 * @author Douglas Teoh
 */
public class ProjectController {

    /** The current project we are working on. */
    private Project project;
    /** The current database we are working on. */
    private MacshapaDatabase db;

    /** The id of the last datacell that was created. */
    private long lastCreatedCellID;
    /** The id of the last selected cell. */
    private long lastSelectedCellID;
    /** The id of the last datacell that was created. */
    private long lastCreatedColID;

    /**
     * Controller state
     */
    /** has the project been changed since it was created. */
    private boolean changed;
    /** Is the project new */
    private boolean newProject;

    public ProjectController() {
        project = new Project();
        changed = false;
        newProject = true;
    }

    public ProjectController(final Project project) {
        this.project = project;
        changed = false;
        newProject = false;
    }

    public void createNewProject(final String name) {
        project = new Project();
        setProjectName(name);
        changed = false;
        newProject = true;
    }

    /**
     * Sets the name of the project.
     * 
     * @param newProjectName
     *            The new name to use for this project.
     */
    public void setProjectName(final String newProjectName) {
        // Check Pre-conditions.
        assert (newProjectName != null);

        // Set the name of the project.
        String name = newProjectName;
        int match = name.lastIndexOf(".shapa");
        if (match != -1) {
            name = name.substring(0, match);
        }
        if (name.equals("")) {
            name = "Project1";
        }

        project.setProjectName(name);
    }

    /**
     * Sets the database associated with this project.
     * 
     * @param newDB
     *            The new database to use with this project.
     */
    public void setDatabase(final MacshapaDatabase newDB) {
        db = newDB;
    }

    /**
     * Gets the database associated with this project.
     * 
     * @return The single database to use with this project.
     */
    public MacshapaDatabase getDB() {
        return db;
    }

    /**
     * @return The id of the last created cell.
     */
    public long getLastCreatedCellId() {
        return lastCreatedCellID;
    }

    /**
     * Sets the id of the last created cell to the specified parameter.
     * 
     * @param newId
     *            The Id of the newly created cell.
     */
    public void setLastCreatedCellId(final long newId) {
        lastCreatedCellID = newId;
    }

    /**
     * @return The id of the last selected cell.
     */
    public long getLastSelectedCellId() {
        return lastSelectedCellID;
    }

    /**
     * Sets the id of the last selected cell to the specified parameter.
     * 
     * @param newId
     *            The id of hte newly selected cell.
     */
    public void setLastSelectedCellId(final long newId) {
        lastSelectedCellID = newId;
    }

    /**
     * @return The id of the last created column.
     */
    public long getLastCreatedColId() {
        return lastCreatedColID;
    }

    /**
     * Sets the id of the last created column to the specified parameter.
     * 
     * @param newId
     *            The Id of the newly created column.
     */
    public void setLastCreatedColId(final long newId) {
        lastCreatedColID = newId;
    }

    /**
     * @return the changed
     */
    public boolean isChanged() {
        return (changed || (db != null && db.isChanged()));
    }

    /**
     * @return the newProject
     */
    public boolean isNewProject() {
        return newProject;
    }

    /**
     * @return the project name
     */
    public String getProjectName() {
        return project.getProjectName();
    }

    /**
     * Set the database file name, directory not included.
     * 
     * @param fileName
     */
    public void setDatabaseFileName(final String fileName) {
        project.setDatabaseFileName(fileName);
    }

    /**
     * @return the database file name, directory not included.
     */
    public String getDatabaseFileName() {
        return project.getDatabaseFileName();
    }

    /**
     * Set the directory the project file (and all project specific resources)
     * resides in.
     * 
     * @param directory
     */
    public void setProjectDirectory(final String directory) {
        project.setProjectDirectory(directory);
    }

    /**
     * @return the directory the project file (and all project specific
     *         resources) resides in.
     */
    public String getProjectDirectory() {
        return project.getProjectDirectory();
    }

    /**
     * Load the settings from the current project.
     */
    public void loadProject() {
        // Use the plugin manager to load up the data viewers
        PluginManager pm = PluginManager.getInstance();
        DataControllerV dataController = OpenSHAPA.getDataController();

        // Load the plugins required for each media file
        boolean showController = false;
        for (ViewerSetting setting : project.getViewerSettings()) {
            showController = true;

            File file = new File(setting.getFilePath());
            Plugin plugin = pm.getAssociatedPlugin(setting.getPluginName());
            if (plugin == null) {
                continue;
            }

            DataViewer viewer = plugin.getNewDataViewer();
            viewer.setDataFeed(file);
            viewer.setOffset(setting.getOffset());

            dataController.addViewer(viewer, setting.getOffset());
            dataController.addTrack(plugin.getTypeIcon(), file
                    .getAbsolutePath(), file.getName(), viewer.getDuration(),
                    setting.getOffset(), -1);
        }

        MixerControllerV mixerController = dataController.getMixerController();
        for (TrackSettings setting : project.getTrackSettings()) {
            mixerController.setTrackInterfaceSettings(setting.getFilePath(),
                    setting.getBookmarkPosition(), setting.isLocked());
        }

        // Show the data controller
        if (showController) {
            OpenSHAPA.getApplication().showDataController();
        }
    }

    /**
     * Gather and update the various project specific settings.
     */
    public void updateProject() {
        if (!changed && !newProject) {
            return;
        }

        DataControllerV dataController = OpenSHAPA.getDataController();

        // Gather the data viewer settings
        List<ViewerSetting> viewerSettings = new LinkedList<ViewerSetting>();
        for (DataViewer viewer : dataController.getDataViewers()) {
            ViewerSetting vs = new ViewerSetting();
            vs.setFilePath(viewer.getDataFeed().getAbsolutePath());
            vs.setOffset(viewer.getOffset());
            vs.setPluginName(viewer.getClass().getName());

            viewerSettings.add(vs);
        }
        project.setViewerSettings(viewerSettings);

        // Gather the user interface settings
        List<TrackSettings> trackSettings = new LinkedList<TrackSettings>();
        for (TrackModel model : dataController.getMixerController()
                .getAllTrackModels()) {
            TrackSettings ts = new TrackSettings();
            ts.setFilePath(model.getTrackId());
            ts.setBookmarkPosition(model.getBookmark());
            ts.setLocked(model.isLocked());

            trackSettings.add(ts);
        }
        project.setTrackSettings(trackSettings);
    }

    /**
     * Marks the project state as being saved.
     */
    public void saveProject() {
        changed = false;
        newProject = false;
    }

    /**
     * Marks the project as being changed. This method will not trigger a
     * project state update.
     */
    public void projectChanged() {
        changed = true;
    }

    /**
     * @return a deep-copy clone of the current project.
     */
    public Project getProject() {
        return project.clone();
    }

}
