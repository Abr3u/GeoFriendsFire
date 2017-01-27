package pt.utl.ist.meic;

import java.util.List;

import pt.utl.ist.meic.DataPoint;

public interface Algorithm {

public void setPoints(List<DataPoint> points);

public void cluster();

}