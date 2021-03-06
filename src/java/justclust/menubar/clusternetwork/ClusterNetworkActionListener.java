package justclust.menubar.clusternetwork;

import edu.umd.cs.piccolo.nodes.PPath;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import justclust.datastructures.Data;
import justclust.JustclustJFrame;
import justclust.datastructures.Cluster;
import justclust.datastructures.Edge;
import justclust.datastructures.Node;
import justclust.ButtonTabComponent;
import justclust.JustclustMouseListener;
import justclust.customcomponents.BrowseButton;
import justclust.datastructures.EdgeGraphicalAttributes;
import justclust.datastructures.NodeGraphicalAttributes;
import justclust.graphdrawing.CustomGraphEditor;
import justclust.plugins.configurationcontrols.CheckBoxControl;
import justclust.plugins.configurationcontrols.ComboBoxControl;
import justclust.plugins.configurationcontrols.FileSystemPathControl;
import justclust.plugins.configurationcontrols.PluginConfigurationControlInterface;
import justclust.plugins.configurationcontrols.DoubleFieldControl;
import justclust.plugins.clustering.ClusteringAlgorithmPluginInterface;
import justclust.plugins.configurationcontrols.IntegerFieldControl;
import justclust.plugins.parsing.FileParserPluginInterface;
import justclust.toolbar.dendrogram.DendrogramCluster;
import justclust.toolbar.dendrogram.DendrogramJDialog;
import justclust.toolbar.filterclusters.FilterClustersJDialog;
import justclust.toolbar.heatmap.HeatMapJDialog;
import justclust.toolbar.microarrayheatmap.MicroarrayHeatMapJDialog;
import justclust.toolbar.networkclusters.NetworkClustersJDialog;
import justclust.toolbar.networkdetails.NetworkDetailsJDialog;
import justclust.toolbar.networkedges.NetworkEdgesJDialog;
import justclust.toolbar.networknodes.NetworkNodesJDialog;
import justclust.toolbar.overrepresentationanalysis.OverrepresentationAnalysisJDialog;
import justclust.toolbar.searchnetwork.SearchNetworkJDialog;

public class ClusterNetworkActionListener implements ActionListener {

    /**
     * This method responds to user input which is made using a
     * ClusterNetworkJDialog.
     */
    public void actionPerformed(ActionEvent actionEvent) {

        if (actionEvent.getActionCommand().equals("comboBoxChanged")) {

            // this method adds the controls for the currently selected
            // plugin into the ClusterNetworkJDialog
            addPluginControls();

        }

        if (actionEvent.getActionCommand().equals("Cluster Network")) {

            ClusterNetworkJDialog.classInstance.clusterNetworkJButton.setEnabled(false);

            // this code starts a new thread to cluster the network.
            // the reason for the new thread is that it allows the progress bar
            // to be animated in the event dispatch thread (this one) while the
            // network is being clustered in the new thread.
            ClusterNetworkThread clusterNetworkThread = new ClusterNetworkThread();
            clusterNetworkThread.start();

        }

    }

    // this method adds the controls for the currently selected
    // plugin into the ClusterNetworkJDialog
    void addPluginControls() {

        // if ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedIndex() > 0,
        // then a plug-in has been selected
        if (ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedIndex() > 0) {

            // the plug-in description is set to contain no characters here so that,
            // if there is an error in loading the description, this description
            // will be used to avoid issues
            String pluginDescription = "";

            try {

                // call the getDescription method of the plug-in to get
                // its description
                File file = new File(Data.clusteringPluginsPath
                        + '/'
                        + Data.clusteringAlgorithmJarNames
                        .get(ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox
                        .getSelectedIndex() - 1));
                URLClassLoader urlClassLoader = URLClassLoader.newInstance(new URL[]{file.toURI().toURL()});
                ClusterNetworkJDialog.classInstance.pluginClass = urlClassLoader.loadClass(Data.clusteringAlgorithmClassNames
                        .get(ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox
                        .getSelectedIndex() - 1));
                ClusterNetworkJDialog.classInstance.pluginClassInstance = ClusterNetworkJDialog.classInstance.pluginClass.newInstance();
                Method method = ClusterNetworkJDialog.classInstance.pluginClass.getMethod("getDescription", new Class[]{});
                pluginDescription = (String) method.invoke(ClusterNetworkJDialog.classInstance.pluginClassInstance, new Object[]{});

            } catch (Exception exception) {

                exception.printStackTrace();

                JOptionPane.showMessageDialog(
                        ClusterNetworkJDialog.classInstance,
                        "Plug-in description could not be loaded due to error");

                return;

            }

            try {

                // call the getConfigurationControls method of the plug-in to get
                // its configuration controls
                File file = new File(Data.clusteringPluginsPath
                        + '/'
                        + Data.clusteringAlgorithmJarNames
                        .get(ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox
                        .getSelectedIndex() - 1));
                URLClassLoader urlClassLoader = URLClassLoader
                        .newInstance(new URL[]{file.toURI().toURL()});
                ClusterNetworkJDialog.classInstance.pluginClass = urlClassLoader.loadClass(Data.clusteringAlgorithmClassNames
                        .get(ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox
                        .getSelectedIndex() - 1));
                ClusterNetworkJDialog.classInstance.pluginClassInstance = ClusterNetworkJDialog.classInstance.pluginClass.newInstance();
                Method method = ClusterNetworkJDialog.classInstance.pluginClass.getMethod("getConfigurationControls",
                        new Class[]{});
                ClusterNetworkJDialog.classInstance.pluginConfigurationControls = (ArrayList<PluginConfigurationControlInterface>) method.invoke(ClusterNetworkJDialog.classInstance.pluginClassInstance, new Object[]{});

            } catch (Exception exception) {

                exception.printStackTrace();

                ClusterNetworkJDialog.classInstance.pluginConfigurationControls = new ArrayList<PluginConfigurationControlInterface>();

                JOptionPane.showMessageDialog(
                        ClusterNetworkJDialog.classInstance,
                        "Plug-in configuration controls could not be loaded due to error");

                return;

            }

            // add the pluginDetailsJLabel, pluginDescriptionJLabel, and
            // pluginDescriptionJScrollPane of the ClusterNetworkJDialog to the
            // clusterNetworkDialogJPanel so that they can display the details of the
            // plug-in.
            // this should only happen if they haven't been added to the
            // clusterNetworkDialogJPanel already.
            if (ClusterNetworkJDialog.classInstance.pluginDetailsJLabel.getParent() != ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel) {
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(ClusterNetworkJDialog.classInstance.pluginDetailsJLabel);
            }
            if (ClusterNetworkJDialog.classInstance.pluginDescriptionJLabel.getParent() != ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel) {
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(ClusterNetworkJDialog.classInstance.pluginDescriptionJLabel);
            }
            if (ClusterNetworkJDialog.classInstance.pluginDescriptionJScrollPane.getParent() != ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel) {
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(ClusterNetworkJDialog.classInstance.pluginDescriptionJScrollPane);
            }

            // update the pluginDescriptionJTextArea to contain the contents of
            // pluginDescription which was assigned earlier
            ClusterNetworkJDialog.classInstance.pluginDescriptionJTextArea.setText(pluginDescription);
            // this makes the scrollbar of the pluginDescriptionJScrollPane start
            // at the top
            ClusterNetworkJDialog.classInstance.pluginDescriptionJTextArea.setCaretPosition(0);

        } else {

            // no plug-in is selected so remove the components which describe
            // plug-ins
            if (ClusterNetworkJDialog.classInstance.pluginDetailsJLabel.getParent() == ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel) {
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.remove(ClusterNetworkJDialog.classInstance.pluginDetailsJLabel);
            }
            if (ClusterNetworkJDialog.classInstance.pluginDescriptionJLabel.getParent() == ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel) {
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.remove(ClusterNetworkJDialog.classInstance.pluginDescriptionJLabel);
            }
            if (ClusterNetworkJDialog.classInstance.pluginDescriptionJScrollPane.getParent() == ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel) {
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.remove(ClusterNetworkJDialog.classInstance.pluginDescriptionJScrollPane);
            }

            // there are no pluginConfigurationControls
            ClusterNetworkJDialog.classInstance.pluginConfigurationControls = new ArrayList<PluginConfigurationControlInterface>();

        }

        // remove any jcomponents which have been added to the
        // ClusterNetworkDialogJPanel by previous plug-ins
        if (ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents != null) {
            for (ArrayList<JComponent> arrayList : ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents) {
                for (JComponent jComponent : arrayList) {
                    ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.remove(jComponent);
                    // if the JComponent is a BrowseButton, it will have had the
                    // ClusterNetworkJDialogMouseActionListener added as a mouse
                    // listener.
                    // the ClusterNetworkJDialogMouseActionListener should be removed.
                    if (jComponent instanceof BrowseButton) {
                        jComponent.removeMouseListener(ClusterNetworkMouseListener.classInstance);
                    }
                }
            }
        }

        // components which correspond to the pluginConfigurationControls
        // are created and added to the ClusterNetworkJDialog
        ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents = new ArrayList<ArrayList<JComponent>>();
        for (PluginConfigurationControlInterface control : ClusterNetworkJDialog.classInstance.pluginConfigurationControls) {
            if (control instanceof CheckBoxControl) {
                ArrayList<JComponent> arrayList = new ArrayList<JComponent>();
                JLabel jLabel = new JLabel(((CheckBoxControl) control).label);
                Font font = new Font("Dialog", Font.PLAIN, 12);
                jLabel.setFont(font);
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(jLabel);
                arrayList.add(jLabel);
                JCheckBox jCheckBox = new JCheckBox();
                jCheckBox.setSelected(((CheckBoxControl) control).checked);
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(jCheckBox);
                arrayList.add(jCheckBox);
                ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents.add(arrayList);
            }
            if (control instanceof ComboBoxControl) {
                ArrayList<JComponent> arrayList = new ArrayList<JComponent>();
                JLabel jLabel = new JLabel(((ComboBoxControl) control).label);
                Font font = new Font("Dialog", Font.PLAIN, 12);
                jLabel.setFont(font);
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(jLabel);
                arrayList.add(jLabel);
                JComboBox jComboBox = new JComboBox();
                jComboBox
                        .setModel(
                        new DefaultComboBoxModel(
                        ((ComboBoxControl) control).options
                        .toArray(new String[((ComboBoxControl) control).options
                        .size()])));
                jComboBox.setSelectedIndex(((ComboBoxControl) control).selectedOptionIndex);
                jComboBox.setFont(font);
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(jComboBox);
                arrayList.add(jComboBox);
                ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents.add(arrayList);
            }
            if (control instanceof DoubleFieldControl) {
                ArrayList<JComponent> arrayList = new ArrayList<JComponent>();
                JLabel jLabel = new JLabel(((DoubleFieldControl) control).label);
                Font font = new Font("Dialog", Font.PLAIN, 12);
                jLabel.setFont(font);
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(jLabel);
                arrayList.add(jLabel);
                JTextField jTextField = new JTextField(String.valueOf(((DoubleFieldControl) control).value));
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(jTextField);
                arrayList.add(jTextField);
                ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents.add(arrayList);
            }
            if (control instanceof FileSystemPathControl) {
                ArrayList<JComponent> arrayList = new ArrayList<JComponent>();
                JLabel jLabel = new JLabel(((FileSystemPathControl) control).label);
                Font font = new Font("Dialog", Font.PLAIN, 12);
                jLabel.setFont(font);
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(jLabel);
                arrayList.add(jLabel);
                JTextField jTextField = new JTextField(((FileSystemPathControl) control).text);
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(jTextField);
                arrayList.add(jTextField);
                BrowseButton browseButton = new BrowseButton();
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(browseButton);
                browseButton.addMouseListener(ClusterNetworkMouseListener.classInstance);
                arrayList.add(browseButton);
                ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents.add(arrayList);
            }
            if (control instanceof IntegerFieldControl) {
                ArrayList<JComponent> arrayList = new ArrayList<JComponent>();
                JLabel jLabel = new JLabel(((IntegerFieldControl) control).label);
                Font font = new Font("Dialog", Font.PLAIN, 12);
                jLabel.setFont(font);
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(jLabel);
                arrayList.add(jLabel);
                JTextField jTextField = new JTextField(String.valueOf(((IntegerFieldControl) control).value));
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(jTextField);
                arrayList.add(jTextField);
                ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents.add(arrayList);
            }
        }

        // pluginDetailsVerticalDisplacement is based on how many components are added to the
        // ClusterNetworkJDialog by plug-ins to configure and describe them.
        // if components have been added, this affects the vertical positioning
        // of all the components in the ClusterNetworkJDialog.
        int pluginDetailsVerticalDisplacement = 0;
        // a positive selected index for the clusteringAlgorithmJComboBox indicates that a
        // plug-in has been selected and therefore some components have been
        // added to the ClusterNetworkJDialog to discribe the plug-in
        if (ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedIndex() > 0) {
            pluginDetailsVerticalDisplacement = 1 + 10 + 25 + 10 + 25 + 10 + 60 + 10 + (25 + 10 + 25 + 10) * ClusterNetworkJDialog.classInstance.pluginConfigurationControls.size() + 1 + 10;
        }

        // set the y coordinate of the ClusterNetworkJDialog so that it remains
        // centered around the point it currently is around when its height
        // is increased.
        // the difference in height between the new height and old height
        // is halved and taken away from the current y coordinate of the
        // ClusterNetworkJDialog.
        // also, increase the height of the ClusterNetworkJDialog.
        ClusterNetworkJDialog.classInstance.setBounds(
                ClusterNetworkJDialog.classInstance.getLocation().x,
                ClusterNetworkJDialog.classInstance.getLocation().y
                - Math.round((10 + 16 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 1 + 10 + pluginDetailsVerticalDisplacement + 25 + 10
                - ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.getHeight()) / 2),
                ClusterNetworkJDialog.classInstance.getWidth(),
                ClusterNetworkJDialog.classInstance.getInsets().top
                + 10 + 16 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 1 + 10 + pluginDetailsVerticalDisplacement + 25 + 10
                + ClusterNetworkJDialog.classInstance.getInsets().bottom);

        // the position of the new components is set.
        // this is necessary if the ClusterNetworkJDialog was not resized above.
        ClusterNetworkJDialog.classInstance.clusterNetworkJDialogComponentListener.componentResized(null);

    }

    /**
     * This inner class has a method which clusters the current network with a
     * new thread.
     */
    class ClusterNetworkThread extends Thread {

        /**
         * This method clusters the current network with a new thread.
         */
        public void run() {

            // the clusterNetworkJProgressBar is added to the
            // clusterNetworkDialogJPanel
            ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.add(ClusterNetworkJDialog.classInstance.clusterNetworkJProgressBar);

            // if ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedIndex() > 0,
            // then a plug-in has been selected
            if (ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedIndex() > 0) {
                // set the y coordinate of the ClusterNetworkJDialog so that it remains
                // centered around the point it currently is around when its height
                // is increased.
                // the difference in height between the new height and old height
                // is halved and taken away from the current y coordinate of the
                // ClusterNetworkJDialog.
                // also, increase the height of the ClusterNetworkJDialog.
                ClusterNetworkJDialog.classInstance.setBounds(
                        ClusterNetworkJDialog.classInstance.getLocation().x,
                        ClusterNetworkJDialog.classInstance.getLocation().y
                        - Math.round((10 + 16 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 1 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 60 + 10 + (25 + 10 + 25 + 10) * ClusterNetworkJDialog.classInstance.pluginConfigurationControls.size() + 1 + 10 + 25 + 10 + 25 + 10
                        - ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.getHeight()) / 2),
                        ClusterNetworkJDialog.classInstance.getWidth(),
                        ClusterNetworkJDialog.classInstance.getInsets().top
                        + 10 + 16 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 1 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 60 + 10 + (25 + 10 + 25 + 10) * ClusterNetworkJDialog.classInstance.pluginConfigurationControls.size() + 1 + 10 + 25 + 10 + 25 + 10
                        + ClusterNetworkJDialog.classInstance.getInsets().bottom);
            } else {
                // set the y coordinate of the ClusterNetworkJDialog so that it remains
                // centered around the point it currently is around when its height
                // is increased.
                // the difference in height between the new height and old height
                // is halved and taken away from the current y coordinate of the
                // ClusterNetworkJDialog.
                // also, increase the height of the ClusterNetworkJDialog.
                ClusterNetworkJDialog.classInstance.setBounds(
                        ClusterNetworkJDialog.classInstance.getLocation().x,
                        ClusterNetworkJDialog.classInstance.getLocation().y
                        - Math.round((10 + 16 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 1 + 10 + 25 + 10 + 25 + 10
                        - ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.getHeight()) / 2),
                        ClusterNetworkJDialog.classInstance.getWidth(),
                        ClusterNetworkJDialog.classInstance.getInsets().top
                        + 10 + 16 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 1 + 10 + 25 + 10 + 25 + 10
                        + ClusterNetworkJDialog.classInstance.getInsets().bottom);
            }

            // activate the clusterNetworkJProgressBar to indicate that some
            // process is running
            ClusterNetworkJDialog.classInstance.clusterNetworkJProgressBar.setIndeterminate(true);

            ClusterNetworkJDialog.classInstance.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            JustclustJFrame.classInstance.statusBarJLabel.setText("Creating clustering...");

            // get the current Data instance for the code later in this method
            // to use.
            // a new instance of the Data class is created which is a copy of
            // the current instance, and the new instance is added to the list
            // of Data instances.
            // all the network data must be copied from the old instance to the
            // new instance.

            int currentCustomGraphEditorIndex = JustclustJFrame.classInstance.justclustJTabbedPane.getSelectedIndex();
            Data data = new Data();

            data.graphShown = Data.data.get(currentCustomGraphEditorIndex).graphShown;

            // the Nodes are copied
            data.networkNodes = new ArrayList<Node>();
            for (Node node : Data.data.get(currentCustomGraphEditorIndex).networkNodes) {
                Node nodeCopy = new Node();
                nodeCopy.label = node.label;
                if (data.graphShown) {
                    nodeCopy.nodeGraphicalAttributes = new NodeGraphicalAttributes();
                    nodeCopy.nodeGraphicalAttributes.visible = node.nodeGraphicalAttributes.visible;
                    nodeCopy.nodeGraphicalAttributes.colour = node.nodeGraphicalAttributes.colour;
                    nodeCopy.nodeGraphicalAttributes.otherVersions = (ArrayList<Node>) node.nodeGraphicalAttributes.otherVersions.clone();
                    for (Node otherVersion : nodeCopy.nodeGraphicalAttributes.otherVersions) {
                        otherVersion.nodeGraphicalAttributes.otherVersions.add(nodeCopy);
                    }
                    node.nodeGraphicalAttributes.otherVersions.add(nodeCopy);
                    nodeCopy.nodeGraphicalAttributes.otherVersions.add(node);
                }
                nodeCopy.data = data;
                nodeCopy.equivalentNodeInOriginalNetwork = node;
                nodeCopy.microarrayValues = node.microarrayValues;
                data.networkNodes.add(nodeCopy);
            }

            // the Edges are copied
            data.networkEdges = new ArrayList<Edge>();
            for (Edge edge : Data.data.get(currentCustomGraphEditorIndex).networkEdges) {
                Edge edgeCopy = new Edge();
                edgeCopy.node1 = data.networkNodes.get(Data.data.get(currentCustomGraphEditorIndex).networkNodes.indexOf(edge.node1));
                edgeCopy.node2 = data.networkNodes.get(Data.data.get(currentCustomGraphEditorIndex).networkNodes.indexOf(edge.node2));
                edgeCopy.weight = edge.weight;
                if (data.graphShown) {
                    edgeCopy.edgeGraphicalAttributes = new EdgeGraphicalAttributes();
                    edgeCopy.edgeGraphicalAttributes.label = edge.edgeGraphicalAttributes.label;
                    edgeCopy.edgeGraphicalAttributes.visible = edge.edgeGraphicalAttributes.visible;
                    edgeCopy.edgeGraphicalAttributes.colour = edge.edgeGraphicalAttributes.colour;
                    edgeCopy.edgeGraphicalAttributes.otherVersions = (ArrayList<Edge>) edge.edgeGraphicalAttributes.otherVersions.clone();
                    for (Edge otherVersion : edgeCopy.edgeGraphicalAttributes.otherVersions) {
                        otherVersion.edgeGraphicalAttributes.otherVersions.add(edgeCopy);
                    }
                    edge.edgeGraphicalAttributes.otherVersions.add(edgeCopy);
                    edgeCopy.edgeGraphicalAttributes.otherVersions.add(edge);
                }
                edgeCopy.data = data;
                data.networkEdges.add(edgeCopy);
            }

            // iterate through each Node and create an ArrayList of Edges
            // for the Node
            for (Node node : data.networkNodes) {
                node.edges = new ArrayList<Edge>();
            }
            // iterate through each Edge and, for each Edge, iterate
            // through each Node.
            // add the Edge to the Node's list of Edges.
            for (Edge edge : data.networkEdges) {
                edge.node1.edges.add(edge);
                // check that the edge isn't a loop and therefore already added
                if (edge.node1 != edge.node2) {
                    edge.node2.edges.add(edge);
                }
            }

            // the other fields are copied
            data.networkClusters = null;
            data.fileName = Data.data.get(currentCustomGraphEditorIndex).fileName;
            data.fileParser = Data.data.get(currentCustomGraphEditorIndex).fileParser;
            data.clusteringAlgorithm = null;
            data.timeTakenToCreateNetwork = Data.data.get(currentCustomGraphEditorIndex).timeTakenToCreateNetwork;
            data.timeTakenToClusterNetwork = 0;
            data.microarrayData = Data.data.get(currentCustomGraphEditorIndex).microarrayData;
            data.microarrayHeaders = Data.data.get(currentCustomGraphEditorIndex).microarrayHeaders;

            // This code sorts the Edges in the Data.networkEdges data
            // structure from largest weight field to smallest weight field.
            for (int i = 1; i < data.networkEdges.size(); i++) {
                Edge edge = data.networkEdges.get(i);
                int j;
                for (j = i - 1; j >= 0 && edge.weight > data.networkEdges.get(j).weight; j--) {
                    data.networkEdges.set(j + 1, data.networkEdges.get(j));
                }
                data.networkEdges.set(j + 1, edge);
            }

            int i = 0;
            try {

                // pass the user specified information for configuring the
                // plug-in to the plug-in by updating fields of the
                // PluginConfigurationControls based on the contents of the
                // corresponding JComponents
                for (; i < ClusterNetworkJDialog.classInstance.pluginConfigurationControls.size(); i++) {
                    if (ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i) instanceof CheckBoxControl) {
                        ((CheckBoxControl) ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i)).checked = ((JCheckBox) ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents.get(i).get(1)).isSelected();
                    }
                    if (ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i) instanceof ComboBoxControl) {
                        ((ComboBoxControl) ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i)).selectedOptionIndex = ((JComboBox) ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents.get(i).get(1)).getSelectedIndex();
                    }
                    if (ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i) instanceof DoubleFieldControl) {
                        ((DoubleFieldControl) ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i)).value = Double.parseDouble(((JTextField) ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents.get(i).get(1)).getText());
                    }
                    if (ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i) instanceof FileSystemPathControl) {
                        File file = new File(((JTextField) ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents.get(i).get(1)).getText());
                        if (!file.exists()) {
                            throw new Exception();
                        }
                        boolean directoriesOnly = ((FileSystemPathControl) ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i)).directoriesOnly;
                        if (!directoriesOnly && file.isDirectory()) {
                            throw new Exception();
                        }
                        if (directoriesOnly && file.isFile()) {
                            throw new Exception();
                        }
                        ((FileSystemPathControl) ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i)).text = ((JTextField) ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents.get(i).get(1)).getText();
                    }
                    if (ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i) instanceof IntegerFieldControl) {
                        ((IntegerFieldControl) ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i)).value = Integer.parseInt(((JTextField) ClusterNetworkJDialog.classInstance.pluginConfigurationJComponents.get(i).get(1)).getText());
                    }
                }

            } catch (Exception exception) {

                ClusterNetworkJDialog.classInstance.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                JustclustJFrame.classInstance.statusBarJLabel.setText("");

                // the clusterNetworkJProgressBar is removed from the
                // clusterNetworkDialogJPanel
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.remove(ClusterNetworkJDialog.classInstance.clusterNetworkJProgressBar);

                // if ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedIndex() > 0,
                // then a plug-in has been selected
                if (ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedIndex() > 0) {
                    // set the y coordinate of the ClusterNetworkJDialog so that it remains
                    // centered around the point it currently is around when its height
                    // is decreased.
                    // the difference in height between the new height and old height
                    // is halved and added to the current y coordinate of the
                    // ClusterNetworkJDialog.
                    // also, decreased the height of the ClusterNetworkJDialog.
                    ClusterNetworkJDialog.classInstance.setBounds(
                            ClusterNetworkJDialog.classInstance.getLocation().x,
                            ClusterNetworkJDialog.classInstance.getLocation().y
                            + Math.round((10 + 25) / 2),
                            ClusterNetworkJDialog.classInstance.getWidth(),
                            ClusterNetworkJDialog.classInstance.getInsets().top
                            + 10 + 16 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 1 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 60 + 10 + (25 + 10 + 25 + 10) * ClusterNetworkJDialog.classInstance.pluginConfigurationControls.size() + 1 + 10 + 25 + 10
                            + ClusterNetworkJDialog.classInstance.getInsets().bottom);
                } else {
                    // set the y coordinate of the ClusterNetworkJDialog so that it remains
                    // centered around the point it currently is around when its height
                    // is decreased.
                    // the difference in height between the new height and old height
                    // is halved and added to the current y coordinate of the
                    // ClusterNetworkJDialog.
                    // also, decreased the height of the ClusterNetworkJDialog.
                    ClusterNetworkJDialog.classInstance.setBounds(
                            ClusterNetworkJDialog.classInstance.getLocation().x,
                            ClusterNetworkJDialog.classInstance.getLocation().y
                            + Math.round((10 + 25) / 2),
                            ClusterNetworkJDialog.classInstance.getWidth(),
                            ClusterNetworkJDialog.classInstance.getInsets().top
                            + 10 + 16 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 1 + 10 + 25 + 10
                            + ClusterNetworkJDialog.classInstance.getInsets().bottom);
                }

                ClusterNetworkJDialog.classInstance.clusterNetworkJButton.setEnabled(true);

                if (ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i) instanceof DoubleFieldControl) {
                    JOptionPane.showMessageDialog(JustclustJFrame.classInstance, "The field labelled \"" + ((DoubleFieldControl) ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i)).label + "\" requires a double");
                }
                if (ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i) instanceof FileSystemPathControl) {
                    boolean directoriesOnly = ((FileSystemPathControl) ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i)).directoriesOnly;
                    if (!directoriesOnly) {
                        JOptionPane.showMessageDialog(JustclustJFrame.classInstance, "The field labelled \"" + ((FileSystemPathControl) ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i)).label + "\" requires a file");
                    }
                    if (directoriesOnly) {
                        JOptionPane.showMessageDialog(JustclustJFrame.classInstance, "The field labelled \"" + ((FileSystemPathControl) ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i)).label + "\" requires a directory");
                    }
                }
                if (ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i) instanceof IntegerFieldControl) {
                    JOptionPane.showMessageDialog(JustclustJFrame.classInstance, "The field labelled \"" + ((IntegerFieldControl) ClusterNetworkJDialog.classInstance.pluginConfigurationControls.get(i)).label + "\" requires an integer");
                }

                return;

            }

            try {

                // This code calls the clusterNetwork method of the class file
                // for the current clustering algorithm.
                Method method = ClusterNetworkJDialog.classInstance.pluginClass.getMethod("clusterNetwork",
                        new Class[]{ArrayList.class, ArrayList.class, ArrayList.class});
                data.networkClusters = new ArrayList<Cluster>();
                long clusteringAlgorithmStartTime = new Date().getTime();
                method.invoke(ClusterNetworkJDialog.classInstance.pluginClassInstance,
                        new Object[]{data.networkNodes, data.networkEdges, data.networkClusters});
                data.timeTakenToClusterNetwork = new Date().getTime() - clusteringAlgorithmStartTime;

                method = ClusterNetworkJDialog.classInstance.pluginClass.getMethod(
                        "isHierarchicalClustering", new Class[]{});
                data.hierarchicalClustering = (boolean) method.invoke(
                        ClusterNetworkJDialog.classInstance.pluginClassInstance, new Object[]{});

                method = ClusterNetworkJDialog.classInstance.pluginClass.getMethod(
                        "getRootDendrogramClusters", new Class[]{});
                data.rootDendrogramClusters = (ArrayList<DendrogramCluster>) method.invoke(
                        ClusterNetworkJDialog.classInstance.pluginClassInstance, new Object[]{});

            } catch (Exception exception) {

                ClusterNetworkJDialog.classInstance.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                JustclustJFrame.classInstance.statusBarJLabel.setText("");

                // the clusterNetworkJProgressBar is removed from the
                // clusterNetworkDialogJPanel
                ClusterNetworkJDialog.classInstance.clusterNetworkDialogJPanel.remove(ClusterNetworkJDialog.classInstance.clusterNetworkJProgressBar);

                // if ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedIndex() > 0,
                // then a plug-in has been selected
                if (ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedIndex() > 0) {
                    // set the y coordinate of the ClusterNetworkJDialog so that it remains
                    // centered around the point it currently is around when its height
                    // is decreased.
                    // the difference in height between the new height and old height
                    // is halved and added to the current y coordinate of the
                    // ClusterNetworkJDialog.
                    // also, decreased the height of the ClusterNetworkJDialog.
                    ClusterNetworkJDialog.classInstance.setBounds(
                            ClusterNetworkJDialog.classInstance.getLocation().x,
                            ClusterNetworkJDialog.classInstance.getLocation().y
                            + Math.round((10 + 25) / 2),
                            ClusterNetworkJDialog.classInstance.getWidth(),
                            ClusterNetworkJDialog.classInstance.getInsets().top
                            + 10 + 16 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 1 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 60 + 10 + (25 + 10 + 25 + 10) * ClusterNetworkJDialog.classInstance.pluginConfigurationControls.size() + 1 + 10 + 25 + 10
                            + ClusterNetworkJDialog.classInstance.getInsets().bottom);
                } else {
                    // set the y coordinate of the ClusterNetworkJDialog so that it remains
                    // centered around the point it currently is around when its height
                    // is decreased.
                    // the difference in height between the new height and old height
                    // is halved and added to the current y coordinate of the
                    // ClusterNetworkJDialog.
                    // also, decreased the height of the ClusterNetworkJDialog.
                    ClusterNetworkJDialog.classInstance.setBounds(
                            ClusterNetworkJDialog.classInstance.getLocation().x,
                            ClusterNetworkJDialog.classInstance.getLocation().y
                            + Math.round((10 + 25) / 2),
                            ClusterNetworkJDialog.classInstance.getWidth(),
                            ClusterNetworkJDialog.classInstance.getInsets().top
                            + 10 + 16 + 10 + 1 + 10 + 25 + 10 + 25 + 10 + 1 + 10 + 25 + 10
                            + ClusterNetworkJDialog.classInstance.getInsets().bottom);
                }

                ClusterNetworkJDialog.classInstance.clusterNetworkJButton.setEnabled(true);

                JOptionPane.showMessageDialog(JustclustJFrame.classInstance, "Clustering could not be completed due to error");

                return;

            }

            if (data.networkEdges.size() > 1000000
                    && JOptionPane.showOptionDialog(
                    JustclustJFrame.classInstance,
                    "Including edges in this clustering may not be possible due to the amount of edges that there would be.\n"
                    + "Would you like to continue without including the edges?",
                    "",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Yes", "No"},
                    "Yes") == 0) {
                for (Node node : data.networkNodes) {
                    node.edges = new ArrayList<Edge>();
                }
                data.networkEdges.clear();
            }

            // remove any open dialogs
            if (NetworkDetailsJDialog.classInstance != null) {
                NetworkDetailsJDialog.classInstance.dispose();
            }
            if (NetworkNodesJDialog.classInstance != null) {
                NetworkNodesJDialog.classInstance.dispose();
            }
            if (NetworkEdgesJDialog.classInstance != null) {
                NetworkEdgesJDialog.classInstance.dispose();
            }
            if (NetworkClustersJDialog.classInstance != null) {
                NetworkClustersJDialog.classInstance.dispose();
            }
            if (SearchNetworkJDialog.classInstance != null) {
                SearchNetworkJDialog.classInstance.dispose();
            }
            if (FilterClustersJDialog.classInstance != null) {
                FilterClustersJDialog.classInstance.dispose();
            }
            if (OverrepresentationAnalysisJDialog.classInstance != null) {
                OverrepresentationAnalysisJDialog.classInstance.dispose();
            }
            if (HeatMapJDialog.classInstance != null) {
                HeatMapJDialog.classInstance.dispose();
            }
            if (MicroarrayHeatMapJDialog.classInstance != null) {
                MicroarrayHeatMapJDialog.classInstance.dispose();
            }
            if (DendrogramJDialog.classInstance != null) {
                DendrogramJDialog.classInstance.dispose();
            }

            // the new Data instance is added to the list
            Data.data.add(data);

            // set each Node's cluster field with the Cluster which contains it.
            // if more than one Cluster contains the Node, copy the Node so that
            // each Cluster contains one copy.
            // this is done so that overlapping Clusters can be represented
            // separately.
            for (Node node : data.networkNodes) {
                node.cluster = null;
            }
            for (Cluster cluster : data.networkClusters) {
                for (int j = 0; j < cluster.nodes.size(); j++) {

                    Node node = cluster.nodes.get(j);

                    if (node.cluster == null) {
                        node.cluster = cluster;
                    } else {
                        Node nodeCopy = new Node();
                        nodeCopy.edges = new ArrayList<Edge>();
                        for (int k = 0; k < node.edges.size(); k++) {

                            Edge edge = node.edges.get(k);

                            // when Nodes are duplicated because of overlapping
                            // Clusters, the Node's Edges are also duplicated
                            Edge edgeCopy = new Edge();
                            if (edge.node1 == node && edge.node2 == node) {
                                edgeCopy.node1 = nodeCopy;
                                edgeCopy.node2 = nodeCopy;
                            } else if (edge.node1 == node) {
                                edgeCopy.node1 = nodeCopy;
                                edgeCopy.node2 = edge.node2;
                                edge.node2.edges.add(edgeCopy);
                            } else {
                                edgeCopy.node1 = edge.node1;
                                edgeCopy.node2 = nodeCopy;
                                edge.node1.edges.add(edgeCopy);
                            }
                            edgeCopy.weight = edge.weight;
                            if (data.graphShown) {
                                edgeCopy.edgeGraphicalAttributes = new EdgeGraphicalAttributes();
                                edgeCopy.edgeGraphicalAttributes.label = edge.edgeGraphicalAttributes.label;
                                edgeCopy.edgeGraphicalAttributes.visible = edge.edgeGraphicalAttributes.visible;
                                edgeCopy.edgeGraphicalAttributes.colour = edge.edgeGraphicalAttributes.colour;
                                edgeCopy.edgeGraphicalAttributes.otherVersions = (ArrayList<Edge>) edge.edgeGraphicalAttributes.otherVersions.clone();
                                for (Edge otherVersion : edgeCopy.edgeGraphicalAttributes.otherVersions) {
                                    otherVersion.edgeGraphicalAttributes.otherVersions.add(edgeCopy);
                                }
                                edge.edgeGraphicalAttributes.otherVersions.add(edgeCopy);
                                edgeCopy.edgeGraphicalAttributes.otherVersions.add(edge);
                            }
                            edgeCopy.data = data;
                            nodeCopy.edges.add(edgeCopy);
                            data.networkEdges.add(data.networkEdges.indexOf(edge) + 1, edgeCopy);

                        }
                        nodeCopy.cluster = cluster;
                        nodeCopy.label = node.label;
                        if (data.graphShown) {
                            nodeCopy.nodeGraphicalAttributes = new NodeGraphicalAttributes();
                            nodeCopy.nodeGraphicalAttributes.visible = node.nodeGraphicalAttributes.visible;
                            nodeCopy.nodeGraphicalAttributes.colour = node.nodeGraphicalAttributes.colour;
                            nodeCopy.nodeGraphicalAttributes.otherVersions = (ArrayList<Node>) node.nodeGraphicalAttributes.otherVersions.clone();
                            for (Node otherVersion : nodeCopy.nodeGraphicalAttributes.otherVersions) {
                                otherVersion.nodeGraphicalAttributes.otherVersions.add(nodeCopy);
                            }
                            node.nodeGraphicalAttributes.otherVersions.add(nodeCopy);
                            nodeCopy.nodeGraphicalAttributes.otherVersions.add(node);
                        }
                        nodeCopy.data = data;
                        nodeCopy.equivalentNodeInOriginalNetwork = node.equivalentNodeInOriginalNetwork;
                        data.networkNodes.add(data.networkNodes.indexOf(node) + 1, nodeCopy);
                        cluster.nodes.remove(j);
                        cluster.nodes.add(j, nodeCopy);
                    }

                }
            }
//            // if two Nodes, which are connected by an Edge, are
//            // both duplicated, there will be four copies of the
//            // edge.
//            // the two Edge copies which are between an original
//            // Node and a Node copy are removed here.
//            // this is so that the graph is not messy.
//            for (int k = 0; k < data.networkEdges.size(); k++) {
//
//                System.out.println(k);
//
//                Edge edge = data.networkEdges.get(k);
//
//                boolean removeRedundantEdgeCopies = false;
//                for (Edge otherVersion : edge.otherVersions) {
//                    if (otherVersion.data == data
//                            && otherVersion.node1 != edge.node1
//                            && otherVersion.node2 != edge.node2) {
//                        removeRedundantEdgeCopies = true;
//                        break;
//                    }
//                }
//                if (removeRedundantEdgeCopies) {
//                    for (int k = 0; k < edge.otherVersions.size(); k++) {
//
//                        System.out.println(k);
//
//                        Edge otherVersion = edge.otherVersions.get(k);
//
//                        if (otherVersion.node1 == edge.node1 || otherVersion.node2 == edge.node2) {
//                            otherVersion.node1.edges.remove(otherVersion);
//                            otherVersion.node2.edges.remove(otherVersion);
//                            if (otherVersion.data.networkEdges.indexOf(otherVersion) < k) {
//                                k--;
//                            }
//                            otherVersion.data.networkEdges.remove(otherVersion);
//                            for (Edge otherVersionOtherVersion : otherVersion.otherVersions) {
//                                otherVersionOtherVersion.otherVersions.remove(otherVersion);
//                            }
//                            k--;
//                        }
//                    }
//                }
//
//            }

            // remove Nodes and Edges which do not belong to the clustering.
            // this relies on the previous code setting the cluster field of
            // Nodes which do not belong to the clustering to null, and so must
            // be performed after it.
            int networkNodesAmount = data.networkNodes.size();
            for (int j = 0; j < networkNodesAmount; j++) {
                Node node = data.networkNodes.get(j);
                if (node.cluster == null) {
                    if (data.graphShown) {
                        for (Node otherVersion : data.networkNodes.get(j).nodeGraphicalAttributes.otherVersions) {
                            otherVersion.nodeGraphicalAttributes.otherVersions.remove(data.networkNodes.get(j));
                        }
                    }
                    data.networkNodes.remove(j);
                    networkNodesAmount--;
                    j--;
                }
            }
            int networkEdgesAmount = data.networkEdges.size();
            for (int j = 0; j < networkEdgesAmount; j++) {
                Edge edge = data.networkEdges.get(j);
                if (edge.node1.cluster != edge.node2.cluster
                        || edge.node1.cluster == null
                        || edge.node2.cluster == null) {
                    edge.node1.edges.remove(edge);
                    edge.node2.edges.remove(edge);
                    if (data.graphShown) {
                        for (Edge otherVersion : edge.edgeGraphicalAttributes.otherVersions) {
                            otherVersion.edgeGraphicalAttributes.otherVersions.remove(edge);
                        }
                    }
                    data.networkEdges.remove(j);
                    networkEdgesAmount--;
                    j--;
                }
            }

            // This code sorts the Clusters in the networkClusters data
            // structure from largest amount of Nodes to smallest amount of
            // Nodes so that they are displayed in this order
            for (int j = 1; j < data.networkClusters.size(); j++) {
                Cluster cluster = data.networkClusters.get(j);
                int k;
                for (k = j - 1; k >= 0 && cluster.nodes.size() > data.networkClusters.get(k).nodes.size(); k--) {
                    data.networkClusters.set(k + 1, data.networkClusters.get(k));
                }
                data.networkClusters.set(k + 1, cluster);
            }

            // add a new Graph to the justclustTabbedPane.
            // this will be used to show the cluster.
            CustomGraphEditor customGraphEditor = new CustomGraphEditor(data.networkNodes, data.networkEdges, data.networkClusters, data.graphShown);
            JustclustJFrame.classInstance.customGraphEditors.add(customGraphEditor);
            // register the customGraphEditor with a
            // JustclustActionListener so that the
            // JustclustActionListener can respond to buttons on the main
            // graphical view being clicked
            JustclustMouseListener justclustMouseListener = new JustclustMouseListener();
            customGraphEditor.addMouseListener(justclustMouseListener);
            // passing an empty String to the setToolTipText method enables tooltips
            // for customGraphEditor
            customGraphEditor.setToolTipText("");
            String title = String.valueOf(ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedItem());
            if (title.length() > 15) {
                title = title.substring(0, 12) + "...";
            }
            JustclustJFrame.classInstance.tabTitles.add(String.valueOf(ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedItem()));
            JustclustJFrame.classInstance.justclustJTabbedPane.addTab(title, customGraphEditor);
            int graphAmount = JustclustJFrame.classInstance.customGraphEditors.size();
            JustclustJFrame.classInstance.justclustJTabbedPane.setSelectedIndex(graphAmount - 1);
            ButtonTabComponent buttonTabComponent = new ButtonTabComponent(JustclustJFrame.classInstance.justclustJTabbedPane);
            JustclustJFrame.classInstance.justclustJTabbedPane.setTabComponentAt(graphAmount - 1, buttonTabComponent);
            JustclustJFrame.classInstance.justclustJTabbedPane.setToolTipTextAt(graphAmount - 1,
                    String.valueOf(ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedItem()));

            // the createGraph method produces a graphical representation of the
            // current network for the Graph in the first tab
            customGraphEditor.createGraph();

            if (data.graphShown) {

                // this method shows the current clustering by making all the nodes and
                // edges visible except for edges which connect nodes which do not belong to
                // the same cluster
                customGraphEditor.showClusteringWithNodeAndEdgeVisibility();

                // this method creates a label for each cluster in the current network
                customGraphEditor.createLabelsForClusters();

                // lay out the nodes with the prefuse force layout.
                // the shouldRepaint field is false while the layout is being
                // applied so that the customGraphEditor is not repainted during the
                // layout.
                // this would cause an error for some unkown reason.
                customGraphEditor.shouldRepaint = false;
                customGraphEditor.applyPrefuseForceLayout();
                customGraphEditor.shouldRepaint = true;

                // show the Node and Edge properties which were carried over to the
                // clustering from the network
                customGraphEditor.updateGraphLabels();
                customGraphEditor.updateGraphVisibility();
                customGraphEditor.updateGraphColour();

            }

            // the repaint method updates the buttons on the toolbar so that
            // they are no longer disabled now that there is information about
            // the current clustering
            JustclustJFrame.classInstance.repaint();

            // the clusteringAlgorithms field of the Data class is populated so
            // that the NetworkDetailsJDialog can display its information
            data.clusteringAlgorithm =
                    String.valueOf(ClusterNetworkJDialog.classInstance.clusteringAlgorithmJComboBox.getSelectedItem());

            ClusterNetworkJDialog.classInstance.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            ClusterNetworkJDialog.classInstance.dispose();

            JustclustJFrame.classInstance.statusBarJLabel.setText("Clustering created");

        }
    }
}
