package pt.utl.ist.meic.domain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CheckIn {
	
	private DataPoint mDataPoint;
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
	private Date mDate;
	
	public CheckIn(Date date,DataPoint dataPoint) {
		this.mDataPoint = dataPoint;
		this.mDate = date;
	}

	public DataPoint getDataPoint() {
		return mDataPoint;
	}

	public void setDataPoint(DataPoint dataPoint) {
		this.mDataPoint = mDataPoint;
	}

	public String getDateFormatted() {
		return df.format(mDate);
	}
	
	public Date getDate() {
		return mDate;
	}

	public void setDate(Date date) {
		this.mDate = mDate;
	}

}
