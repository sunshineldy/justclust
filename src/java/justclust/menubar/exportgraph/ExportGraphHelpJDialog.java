/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package justclust.menubar.exportgraph;

import java.awt.Dialog;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import justclust.datastructures.Cluster;
import justclust.datastructures.Data;
import justclust.datastructures.Edge;
import justclust.JustclustJFrame;
import justclust.toolbar.networkdetails.NetworkDetailsComponentListener;
import justclust.toolbar.networkdetails.NetworkDetailsJDialog;
import static justclust.toolbar.networkdetails.NetworkDetailsJDialog.classInstance;

/**
 *
 * @author wuaz008
 */
public class ExportGraphHelpJDialog extends JDialog {

    public static ExportGraphHelpJDialog classInstance;
    public JPanel exportGraphHelpJPanel;
    public JEditorPane exportGraphHelpJEditorPane;
    public JScrollPane exportGraphHelpJScrollPane;
    ExportGraphHelpComponentListener exportGraphHelpComponentListener;

    public ExportGraphHelpJDialog() {

        classInstance = this;

        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("Export Graph Help");
        ImageIcon img = new ImageIcon("img/justclust_icon.png");
        setIconImage(img.getImage());
        setResizable(false);

        exportGraphHelpJPanel = new JPanel();
        add(exportGraphHelpJPanel);
        exportGraphHelpJPanel.setLayout(null);

        exportGraphHelpJEditorPane = new JEditorPane("text/html", "");
        exportGraphHelpJEditorPane.setEditable(false);
        exportGraphHelpJScrollPane = new JScrollPane(exportGraphHelpJEditorPane);
        exportGraphHelpJPanel.add(exportGraphHelpJScrollPane);

        String details = "To export a graph:"
                + "<ol>"
                + "<li>Click on the browse button (folder icon) to the right of the <b>File Name</b> text field and choose a file</li>"
                + "<li>Click on the <b>Export Graph</b> button</li>"
                + "</ol>";
        exportGraphHelpJEditorPane.setText(details);
        // this makes the scrollbar of the exportGraphHelpJScrollPane start
        // at the top
        exportGraphHelpJEditorPane.setCaretPosition(0);

        exportGraphHelpComponentListener = new ExportGraphHelpComponentListener();
        addComponentListener(exportGraphHelpComponentListener);

        // the setBounds method must be called after the
        // exportGraphHelpComponentListener is registered so that the
        // exportGraphHelpJTextArea is always visible within the
        // exportGraphHelpJScrollPane.
        // this is for unkown reasons.
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = g.getScreenDevices();
        ExportGraphHelpJDialog.classInstance.setBounds(
                (int) Math.round(((double) devices[0].getDisplayMode().getWidth() - 500) / 2),
                (int) Math.round(((double) devices[0].getDisplayMode().getHeight() - 250) / 2),
                500,
                250);

        // the setVisible method is called to make the ExportGraphHelpJDialog
        // appear
        setVisible(true);

    }
}
