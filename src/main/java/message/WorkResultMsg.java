package message;

public class WorkResultMsg {
    private String result;
    private WorkOrderMsg workOrderMsg;

    public WorkResultMsg(String result, WorkOrderMsg workOrderMsg) {
        this.result = result;
        this.workOrderMsg = workOrderMsg;
    }

    public String getResult() {
        return result;
    }

    public WorkOrderMsg getWorkOrderMsg() {
        return workOrderMsg;
    }
}
