package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }

        public String toString(){

            return ("la machine " + this.machine+" le first " + firstTask +" le last "+lastTask);
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {
            Task Taskbymachine[][] = order.tasksByMachine;
            Task Aux;
            Aux = order.tasksByMachine[this.machine][this.t1];
            order.tasksByMachine[this.machine][this.t1]=order.tasksByMachine[this.machine][this.t2];
            order.tasksByMachine[this.machine][this.t2]=Aux;
        }
    }




    @Override
    public Result solve(Instance instance, long deadline) {
        EST_LRPT Solution = new EST_LRPT();
        Result Res = Solution.solve(instance,15);
        Schedule schedule= Res.schedule;
        ResourceOrder order = new ResourceOrder(schedule);
        ResourceOrder orderinit = order.copy();
        Boolean changed = true;
        List<Block> Blocks;
        List<Swap> Neighbors;
        int new_makespan = order.toSchedule().makespan();
        int best= schedule.makespan();
        while ((deadline-System.currentTimeMillis()>1)&&(changed)){
            Blocks = blocksOfCriticalPath(order);
            for(int i=0; i<Blocks.size();i++){
                Neighbors = neighbors(Blocks.get(i));
                for(int j=0; j<Neighbors.size();j++){
                    ResourceOrder order1 = order.copy();
                    Neighbors.get(j).applyOn(order1);
                    new_makespan = order1.toSchedule().makespan();
                    if (new_makespan<best){
                        order = order1;
                        best = new_makespan;
                    }
                }
            }
            if(order==orderinit){
                changed=false;
            }
            else {
                orderinit=order;
            }
        }
        return new Result(instance, order.toSchedule(), Result.ExitCause.Blocked);

    }

    public static int getIndex(ResourceOrder order, int machine, Task task){
        int index=0;
        for(int i =0; i<order.tasksByMachine[machine].length;i++){
            if (order.tasksByMachine[machine][i].equals(task)){
                index = i;
            }
        }
        return index;
    }

    /** Returns a list of all blocks of the critical path. */
    public static List<Block> blocksOfCriticalPath(ResourceOrder order) {
        LinkedList<Block> Blocks=new LinkedList<>();
        List<Task> CriticalPath = order.toSchedule().criticalPath();
        Task task= CriticalPath.get(0) ;
        int machine = order.instance.machine(CriticalPath.get(0));
        int first=getIndex(order,machine,task);
        int last=first;

        for (int i =1 ; i< CriticalPath.size();i++){
            task = CriticalPath.get(i);
            if (machine == order.instance.machine(CriticalPath.get(i))){
              last ++;
            }
            else {
                if (first != last){
                    Block b = new Block(machine,first,last);
                    Blocks.add(b);
                }
                machine=order.instance.machine(task);
                first = getIndex(order,machine,task);
                last= first;
            }
        }
        return Blocks;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block) {
        LinkedList<Swap> Swaps=new LinkedList<>();
        Swap s;
        Block b;
        if(block.lastTask-block.firstTask==1){
            Swaps.add(new Swap(block.machine,block.firstTask,block.lastTask));
        }
        else {
            Swaps.add(new Swap(block.machine, block.firstTask,block.firstTask+1 ));
            Swaps.add(new Swap(block.machine,block.lastTask-1, block.lastTask));
        }
        return Swaps;
    }

}
