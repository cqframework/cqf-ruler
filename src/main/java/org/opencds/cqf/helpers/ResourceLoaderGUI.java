package org.opencds.cqf.helpers;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Created by Christopher on 1/17/2017.
 */
public class ResourceLoaderGUI {

    private LoadResourceHelper helper;

    // Panels
    private JPanel singleResourcePanel = new JPanel();
    private JPanel listResourcePanel = new JPanel();
    private JPanel bundleResourcePanel = new JPanel();

    // Labels
    private JLabel sourceURLLabel = new JLabel("Source URL: ");
    private JLabel destURLLabel = new JLabel("Destination URL: ");
    private JLabel resourceIdLabel = new JLabel("Resource ID: ");
    private JLabel resourceTypeLabel = new JLabel("Resource Type: ");
    private JLabel fromFileLabel = new JLabel("File Path: ");

    // Text Fields
    private JTextField sourceURLText = new JTextField("Base URL where Resource is located");
    private JTextField destURLText = new JTextField("Base URL where Resource is being copied");
    private JTextField resourceIdText = new JTextField(25);
    private JTextField resourceTypeText = new JTextField(25);
    private JTextField fromFileText = new JTextField("Path to Bundle JSON file");

    // Text Areas
    private JTextArea listResourcesArea = new JTextArea("FORMAT: comma separated list of resource id followed by resource type\n" +
            "resourceId, resourceType, 123, Condition, Pat-22, Patient, ...");

    // Buttons
    private JButton loadButton = new JButton("Load");

    // Combo Boxes
    private JComboBox<String> loadOperationsCombo = new JComboBox<>(new String[] {"Choose one of the following", "Single resource", "List of resources", "Bundle from file"});
    
    public ResourceLoaderGUI() {
        JFrame frame = new JFrame("Resource Loader");
        frame.setLayout(new BorderLayout());
        JPanel homePanel = new JPanel();
        homePanel.setBorder(new EmptyBorder(0, 10, 0, 0));
        homePanel.setLayout(new BoxLayout(homePanel, BoxLayout.Y_AXIS));
        JPanel sourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel destPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel loadOperationsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        sourcePanel.add(sourceURLLabel, FlowLayout.LEFT);
        sourcePanel.add(sourceURLText);
        destPanel.add(destURLLabel, FlowLayout.LEFT);
        destPanel.add(destURLText);
        loadOperationsCombo.addActionListener(e -> {
            helper = new LoadResourceHelper(sourceURLText.getText(), destURLText.getText());
            frame.remove(homePanel);
            String s = (String) loadOperationsCombo.getSelectedItem();
            switch (s) {
                case "Single resource":
                    singleResource(frame);
                    break;
                case "List of resources":
                    listResource(frame);
                    break;
                case "Bundle from file":
                    bundleResourceFromFile(frame);
                    break;
            }
        });
        
        loadOperationsPanel.add(loadOperationsCombo);
        homePanel.add(sourcePanel);
        homePanel.add(destPanel);
        homePanel.add(loadOperationsPanel);
        frame.add(homePanel);
        frame.setBounds(50, 50, 450, 175);
        frame.setVisible(true);
    }

    public void bundleResourceFromFile(JFrame frame) {
        bundleResourcePanel.setLayout(new BoxLayout(bundleResourcePanel, BoxLayout.Y_AXIS));
        bundleResourcePanel.setBorder(new EmptyBorder(0, 10, 0, 0));
        JPanel fromFilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        fromFilePanel.add(fromFileLabel, new FlowLayout(FlowLayout.LEFT));
        fromFilePanel.add(fromFileText);
        buttonPanel.add(loadButton);
        loadButton.addActionListener(e -> helper.loadBundle(helper.resolveBundle(fromFileText.getText())));

        bundleResourcePanel.add(fromFilePanel);
        frame.add(bundleResourcePanel);
        frame.setVisible(true);
    }

    public void listResource(JFrame frame) {
        listResourcePanel.setLayout(new BoxLayout(listResourcePanel, BoxLayout.Y_AXIS));
        listResourcePanel.setBorder(new EmptyBorder(0, 10, 0, 0));
        JPanel textAreaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        listResourcesArea.setColumns(37);
        listResourcesArea.setLineWrap(true);
        textAreaPanel.add(listResourcesArea);
        buttonPanel.add(loadButton);
        loadButton.addActionListener(e -> {
            String[] content = listResourcesArea.getText().split(",");
            for (int i = 0; i < content.length; ++i) {
                helper.loadResource(helper.getResource(content[i].trim(), content[++i].trim()));
            }
        });

        listResourcePanel.add(textAreaPanel);
        listResourcePanel.add(buttonPanel);
        JScrollPane pane = new JScrollPane(listResourcePanel);
        frame.add(pane);
        frame.setVisible(true);
    }

    public void singleResource(JFrame frame) {
        singleResourcePanel.setLayout(new BoxLayout(singleResourcePanel, BoxLayout.Y_AXIS));
        singleResourcePanel.setBorder(new EmptyBorder(0, 10, 0, 0));
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        idPanel.add(resourceIdLabel, new FlowLayout(FlowLayout.LEFT));
        idPanel.add(resourceIdText);
        typePanel.add(resourceTypeLabel, new FlowLayout(FlowLayout.LEFT));
        typePanel.add(resourceTypeText);
        buttonPanel.add(loadButton);
        loadButton.addActionListener(e -> 
                helper.loadResource(helper.getResource(resourceIdText.getText(), resourceTypeText.getText()))
        );

        singleResourcePanel.add(idPanel);
        singleResourcePanel.add(typePanel);
        singleResourcePanel.add(buttonPanel);
        frame.add(singleResourcePanel);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new ResourceLoaderGUI();
    }
}
