package com.theeralabs.follome.model.directionMatrix.distanceMatrix;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.theeralabs.follome.model.directionMatrix.direction.Distance;

public class Element {

@SerializedName("status")
@Expose
private String status;
@SerializedName("duration")
@Expose
private Duration duration;
@SerializedName("distance")
@Expose
private Distance distance;

public String getStatus() {
return status;
}

public void setStatus(String status) {
this.status = status;
}

public Duration getDuration() {
return duration;
}

public void setDuration(Duration duration) {
this.duration = duration;
}

public Distance getDistance() {
return distance;
}

public void setDistance(Distance distance) {
this.distance = distance;
}

}