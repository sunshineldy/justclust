package justclust.menubar.exportnetwork;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import justclust.datastructures.Data;
import justclust.JustclustJFrame;
import justclust.datastructures.Edge;
import justclust.menubar.filefilters.CSVFileFilter;
import justclust.plugins.configurationcontrols.FileSystemPathControl;

public class ExportNetworkMouseListener implements MouseListener {

    public void mouseClicked(MouseEvent me) {

        if (me.getComponent() == ExportNetworkJDialog.classInstance.exportNetworkHelpButton) {
            new ExportNetworkHelpJDialog();
        }

        if (me.getComponent() == ExportNetworkJDialog.classInstance.browseButton) {

            // this code presents a JFileChooser to the user and then, if the
            // user has chosen a file, adds a CSV file extension to the file if
            // there isn't one, assigns the file to the file field of this
            // class, and populates the text field contained in the
            // ExportNetworkJDialog.fileNameJTextField field with the path of the
            // file
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setAcceptAllFileFilterUsed(false);
            jFileChooser.addChoosableFileFilter(new CSVFileFilter());
            // lastAccessedFile contains the file on the user's hard drive which was
            // last accessed.
            // whenever the user browses their hard drive from within JustClust, this
            // lastAccessedFile will determine the directory which the file chooser
            // opens initially.
            // the user will be taken straight to the directory they last browsed to.
            jFileChooser.setSelectedFile(Data.lastAccessedFile);
            int jFileChooserState = jFileChooser.showSaveDialog(ExportNetworkJDialog.classInstance);
            if (jFileChooserState == JFileChooser.APPROVE_OPTION) {
                Data.lastAccessedFile = jFileChooser.getSelectedFile();
//                ExportNetworkJDialog.classInstance.file = jFileChooser.getSelectedFile();
//                if (!ExportNetworkJDialog.classInstance.file.getAbsolutePath().toLowerCase().endsWith(".csv")) {
//                    ExportNetworkJDialog.classInstance.file = new File(ExportNetworkJDialog.classInstance.file.getAbsolutePath() + ".csv");
//                }
                ExportNetworkJDialog.classInstance.fileNameJTextField.setText(jFileChooser.getSelectedFile().getAbsolutePath());
            }

        }

    }

    public void mousePressed(MouseEvent me) {
    }

    public void mouseReleased(MouseEvent me) {
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }
}
