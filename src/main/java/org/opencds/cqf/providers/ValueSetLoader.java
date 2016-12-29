package org.opencds.cqf.providers;

import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.opencds.cqf.cql.data.fhir.FhirDataProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by Christopher on 12/18/2016.
 */
public class ValueSetLoader {

    // containers
    private Frame mainFrame;
    private Panel contentPanel;

    // labels
    private Label valueSetGetEndpoint;
    private Label valueSetPutEndpoint;
    private Label valueSetId;

    // fields
    private TextField valueSetGetField;
    private TextField valueSetPutField;
    private TextField valueSetIdField;
    private TextArea outputResults;

    // buttons
    private Button loadBtn;

    public ValueSetLoader() {
        mainFrame = new Frame("Cql Evaluation");
        mainFrame.setSize(500, 600);
        mainFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent){
                System.exit(0);
            }
        });

        GridBagLayout bag = new GridBagLayout();
        GridBagConstraints con = new GridBagConstraints();

        contentPanel = new Panel();
        contentPanel.setLayout(bag);

        con.gridwidth = GridBagConstraints.REMAINDER;
        con.anchor = GridBagConstraints.LINE_START;
        valueSetGetEndpoint = new Label("GET endpoint: ");
        bag.setConstraints(valueSetGetEndpoint, con);
        contentPanel.add(valueSetGetEndpoint);

        valueSetGetField = new TextField("URL to where valueset is", 60);
        bag.setConstraints(valueSetGetField, con);
        contentPanel.add(valueSetGetField);

        con.gridwidth = GridBagConstraints.REMAINDER;
        con.anchor = GridBagConstraints.LINE_START;
        valueSetPutEndpoint = new Label("PUT endpoint: ");
        bag.setConstraints(valueSetPutEndpoint, con);
        contentPanel.add(valueSetPutEndpoint);

        valueSetPutField = new TextField("URL to where you want ot put the valueset", 60);
        bag.setConstraints(valueSetPutField, con);
        contentPanel.add(valueSetPutField);

        con.gridwidth = GridBagConstraints.REMAINDER;
        con.anchor = GridBagConstraints.LINE_START;
        valueSetId = new Label("ValueSet Identifier: ");
        bag.setConstraints(valueSetId, con);
        contentPanel.add(valueSetId);

        valueSetIdField = new TextField("", 60);
        bag.setConstraints(valueSetIdField, con);
        contentPanel.add(valueSetIdField);

        con.gridwidth = GridBagConstraints.REMAINDER;
        con.anchor = GridBagConstraints.LINE_START;
        loadBtn = new Button("load");
        loadBtn.addActionListener(ae -> processRequest());
        bag.setConstraints(loadBtn, con);
        contentPanel.add(loadBtn);

        con.gridwidth = GridBagConstraints.REMAINDER;
        con.anchor = GridBagConstraints.LINE_START;
        outputResults = new TextArea("", 20, 60);
        outputResults.setEditable(false);
        contentPanel.add(outputResults);

        mainFrame.add(contentPanel);
        mainFrame.setVisible(true);

    }

    private void processRequest() {

        try {
            FhirDataProvider provider = new FhirDataProvider().withEndpoint(valueSetGetField.getText());
            provider.setTerminologyProvider(new FhirTerminologyProvider().withEndpoint(valueSetGetField.getText()));
            // provider.setExpandValueSets(true);
            ValueSet vs = (ValueSet) provider.getFhirClient().read(new IdDt(valueSetIdField.getText()).withResourceType("ValueSet"));

            vs.setUrl(valueSetPutField.getText() + "/Valueset/" + valueSetIdField.getText());
            vs.getExpansion().setIdentifier(valueSetPutField.getText() + "/Valueset/" + valueSetIdField.getText());

            FhirDataProvider destination = new FhirDataProvider().withEndpoint(valueSetPutField.getText());
            outputResults.setText(destination.getFhirClient().update(new IdDt(valueSetIdField.getText()).withResourceType("ValueSet"), vs).toString());
        } catch (Exception e) {
            outputResults.setText(e.getMessage());
        }

    }

    public static void main(String[] args) {
        new ValueSetLoader();
    }
}
