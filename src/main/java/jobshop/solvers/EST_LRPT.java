package jobshop.solvers;
import java.util.ArrayList;
import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import java.util.Iterator;

public class EST_LRPT implements Solver{
    private int getLRPT(Instance instance, Task task) {
        int longest = 0;
        for(int i = task.task; i<instance.numTasks; i++){
            longest = longest + instance.duration(task.job, i);
        }
        return(longest);
    }

    private Task LRPT(Instance instance, ArrayList<Task> Tasks, int[] dureemachine , int[] dureejob){
        Iterator<Task> iter = Tasks.iterator();
        Task current=iter.next();
        Task nextToDO =current;
        while(iter.hasNext()){
            current = iter.next();
            if(Math.max(dureemachine[instance.machine(current)], dureejob[current.job])<Math.max(dureemachine[instance.machine(nextToDO)], dureejob[nextToDO.job])){
                nextToDO = current;
            }
            else if (Math.max(dureemachine[instance.machine(current)], dureejob[current.job])==Math.max(dureemachine[instance.machine(nextToDO)], dureejob[nextToDO.job])){
                if(getLRPT(instance,current)> getLRPT(instance,nextToDO)){
                    nextToDO = current;
                }
            }
        }
        return nextToDO;
    }

    public Result solve(Instance instance, long deadline) {

        ResourceOrder sol = new ResourceOrder(instance);
        ArrayList<Task> TaskToRealise = new ArrayList<Task>();
        int dureemachine[]= new int[instance.numMachines];
        int dureejob[]=new int[instance.numJobs];

        for (int i = 0; i<instance.numJobs;i++){
            TaskToRealise.add(new Task(i,0));
        }
        int remainingTasks = instance.numJobs*instance.numMachines;
        while (remainingTasks>0){
            Task current = LRPT(instance,TaskToRealise,dureemachine,dureejob);
            int machine = instance.machine(current);
            sol.tasksByMachine[machine][sol.nextFreeSlot[machine]] = current;
            sol.nextFreeSlot[machine]++;
            if(current.task+1<instance.numTasks){
                TaskToRealise.add(new Task(current.job,current.task+1));
            }
            TaskToRealise.remove(current);
            dureemachine[machine]=instance.duration(current)+Math.max(dureemachine[machine],dureejob[current.job]);
            dureejob[current.job]=instance.duration(current)+Math.max(dureemachine[machine],dureejob[current.job]);
            remainingTasks --;
        }
        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }

}
