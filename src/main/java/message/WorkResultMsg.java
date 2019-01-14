package message;

import org.apache.commons.math3.linear.RealVector;

public class WorkResultMsg {
    private RealVector result;
    private WorkOrderMsg workOrderMsg;

    public WorkResultMsg(RealVector result, WorkOrderMsg workOrderMsg) {
        this.result = result;
        this.workOrderMsg = workOrderMsg;
    }

    public RealVector getResult() {
        return result;
    }

    public WorkOrderMsg getWorkOrderMsg() {
        return workOrderMsg;
    }
}
