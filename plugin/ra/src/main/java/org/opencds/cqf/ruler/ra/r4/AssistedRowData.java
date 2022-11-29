package org.opencds.cqf.ruler.ra.r4;

import com.opencsv.bean.CsvBindByName;

@SuppressWarnings("unused")
public class AssistedRowData {
	@CsvBindByName(column = "periodStart")
	private String periodStart;
	@CsvBindByName(column = "periodEnd")
	private String periodEnd;
	@CsvBindByName(column = "modelId")
	private String modelId;
	@CsvBindByName(column = "modelVersion")
	private String modelVersion;
	@CsvBindByName(column = "patientId")
	private String patientId;
	@CsvBindByName(column = "ccCode")
	private String ccCode;
	@CsvBindByName(column = "suspectType")
	private String suspectType;
	@CsvBindByName(column = "evidenceStatus")
	private String evidenceStatus;
	@CsvBindByName(column = "evidenceStatusDate")
	private String evidenceStatusDate;
	@CsvBindByName(column = "hiearchicalStatus")
	private String hiearchicalStatus;

	public String getPeriodStart() {
		return periodStart;
	}

	public String getPeriodEnd() {
		return periodEnd;
	}

	public String getModelId() {
		return modelId;
	}

	public String getModelVersion() {
		return modelVersion;
	}

	public String getPatientId() {
		return patientId;
	}

	public String getCcCode() {
		return ccCode;
	}

	public String getSuspectType() {
		return suspectType;
	}

	public String getEvidenceStatus() {
		return evidenceStatus;
	}

	public String getEvidenceStatusDate() {
		return evidenceStatusDate;
	}

	public String getHiearchicalStatus() {
		return hiearchicalStatus;
	}
}
