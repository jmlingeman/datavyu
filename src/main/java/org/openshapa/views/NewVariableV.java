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
package org.openshapa.views;

import com.usermetrix.jclient.Logger;
import org.openshapa.OpenSHAPA;

import com.usermetrix.jclient.UserMetrix;
import javax.swing.undo.UndoableEdit;
import org.openshapa.models.db.Datastore;
import org.openshapa.models.db.UserWarningException;
import org.openshapa.models.db.Variable;
import org.openshapa.undoableedits.AddVariableEdit;

/**
 * The dialog for users to add new variables to the spreadsheet.
 */
public final class NewVariableV extends OpenSHAPADialog {

    /** The database to add the new variable to. */
    private Datastore model;

    /** The logger for this class. */
    private static final Logger LOGGER = UserMetrix.getLogger(NewVariableV.class);

    /**
     * Constructor, creates a new form to create a new variable.
     *
     * @param parent The parent of this form.
     * @param modal Should the dialog be modal or not?
     */
    public NewVariableV(final java.awt.Frame parent, final boolean modal) {
        super(parent, modal);
        LOGGER.event("newVar - show");
        initComponents();
        setName(this.getClass().getSimpleName());

        model = OpenSHAPA.getProjectController().getDB();

        // init button group
        buttonGroup1.add(textTypeButton);
        buttonGroup1.add(nominalTypeButton);
        buttonGroup1.add(matrixTypeButton);

        getRootPane().setDefaultButton(okButton);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed"
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        nameField = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        textTypeButton = new javax.swing.JRadioButton();
        nominalTypeButton = new javax.swing.JRadioButton();
        matrixTypeButton = new javax.swing.JRadioButton();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/openshapa/views/resources/NewVariableV"); // NOI18N
        setTitle(bundle.getString("title.text")); // NOI18N
        setName("Form"); // NOI18N
        setResizable(false);

        jLabel1.setText(bundle.getString("jLabel2.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText(bundle.getString("nameField.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        nameField.setName("nameField"); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Type:"));
        jPanel1.setName("jPanel1"); // NOI18N

        textTypeButton.setSelected(true);
        textTypeButton.setLabel(bundle.getString("textTypeButton.text")); // NOI18N
        textTypeButton.setName("textTypeButton"); // NOI18N

        nominalTypeButton.setLabel(bundle.getString("nominalTypeButton.text")); // NOI18N
        nominalTypeButton.setName("nominalTypeButton"); // NOI18N

        matrixTypeButton.setLabel(bundle.getString("matrixTypeButton.text")); // NOI18N
        matrixTypeButton.setName("matrixTypeButton"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(39, 39, 39)
                .add(textTypeButton)
                .add(40, 40, 40)
                .add(nominalTypeButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 39, Short.MAX_VALUE)
                .add(matrixTypeButton)
                .add(24, 24, 24))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(textTypeButton)
                    .add(nominalTypeButton)
                    .add(matrixTypeButton))
                .addContainerGap(39, Short.MAX_VALUE))
        );

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.openshapa.OpenSHAPA.class).getContext().getResourceMap(NewVariableV.class);
        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setName("okButton"); // NOI18N
        okButton.setPreferredSize(new java.awt.Dimension(65, 23));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setLabel(bundle.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jLabel2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(layout.createSequentialGroup()
                        .add(okButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelButton))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(nameField))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(nameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel1))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(cancelButton)
                            .add(okButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * The action to invoke when the user selects the OK button.
     *
     * @param evt The event that triggered this action.
     */
    private void okButtonActionPerformed(final java.awt.event.ActionEvent evt) {// GEN-FIRST:event_okButtonActionPerformed
        try {
            LOGGER.event("newVar - create column:" + getVariableType());
            model.createVariable(getVariableName(), getVariableType());

            // record the effect
            UndoableEdit edit = new AddVariableEdit(getVariableName(), getVariableType());

            // Display any changes.
            OpenSHAPA.getApplication().getMainView().getComponent().revalidate();           
            OpenSHAPA.getView().getUndoSupport().postEdit(edit);

            dispose();

        // Whoops, user has done something strange - show warning dialog.
        } catch (UserWarningException fe) {
            OpenSHAPA.getApplication().showWarningDialog(fe);

        }
    }// GEN-LAST:event_okButtonActionPerformed

    /**
     * The action to invoke when the user selects the cancel button.
     * 
     * @param evt The event that triggered this action.
     */
    private void cancelButtonActionPerformed(final java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cancelButtonActionPerformed
        LOGGER.event("newVar - cancel create.");
        dispose();

    }// GEN-LAST:event_cancelButtonActionPerformed

    /**
     * @return The name of the new variable the user has specified.
     */
    public String getVariableName() {
        return nameField.getText();
    }

    /**
     * @return The type of variable the user has selected to use.
     */
    public Variable.type getVariableType() {
        if (nominalTypeButton.isSelected()) {
            return Variable.type.NOMINAL;
        } else if (matrixTypeButton.isSelected()) {
            return Variable.type.MATRIX;
        } else {
            return Variable.type.TEXT;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton matrixTypeButton;
    private javax.swing.JTextField nameField;
    private javax.swing.JRadioButton nominalTypeButton;
    private javax.swing.JButton okButton;
    private javax.swing.JRadioButton textTypeButton;
    // End of variables declaration//GEN-END:variables
}
