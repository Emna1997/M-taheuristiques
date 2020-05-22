package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TabooSolver implements Solver {

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
    }

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

        ResourceOrder currentOrder= new ResourceOrder(schedule);; //Solution courante
        ResourceOrder BestTaboo=currentOrder.copy();
        ResourceOrder order1;

        int staboo[][] = new int[instance.numTasks*instance.numJobs][instance.numTasks*instance.numJobs];

        int dureeTaboo=4;
        int maxiter=100;
        int k=0;
        List<DescentSolver.Block> Blocks;
        List<DescentSolver.Swap> Neighbors;


        while((deadline-System.currentTimeMillis()>1) && (k<= maxiter)){
            k++;
            Blocks = blocksOfCriticalPath(currentOrder);
            for(int i=0; i<Blocks.size();i++){
                Neighbors = neighbors(Blocks.get(i));
                for(int j=0; j<Neighbors.size();j++){
                    order1= currentOrder.copy();
                    if(staboo[Neighbors.get(j).t1][Neighbors.get(j).t2]<k){
                        Neighbors.get(j).applyOn(order1);
                        currentOrder = order1;
                        if(currentOrder.toSchedule().makespan()<BestTaboo.toSchedule().makespan()){
                            BestTaboo=currentOrder;
                        }
                        staboo[Neighbors.get(j).t2][Neighbors.get(j).t1]=k + dureeTaboo;
                    }
                }
            }
        }
        return new Result(instance, BestTaboo.toSchedule(), Result.ExitCause.Blocked);

    }

    public int getIndex(ResourceOrder order, int machine, Task task){
        int index=0;
        for(int i =0; i<order.tasksByMachine[machine].length;i++){
            if (order.tasksByMachine[machine][i].equals(task)){
                index = i;
            }
        }
        return index;
    }

    /** Returns a list of all blocks of the critical path. */
    List<DescentSolver.Block> blocksOfCriticalPath(ResourceOrder order) {
        LinkedList<DescentSolver.Block> Blocks=new LinkedList<>();
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
                    Blocks.add(new DescentSolver.Block(machine,first,last));
                }
                machine=order.instance.machine(task);
                first = getIndex(order,machine,task);
                last= first;
            }
        }
        return Blocks;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<DescentSolver.Swap> neighbors(DescentSolver.Block block) {
        LinkedList<DescentSolver.Swap> Swaps=new LinkedList<>();
        DescentSolver.Swap s;
        DescentSolver.Block b;
        if(block.lastTask-block.firstTask==1){
            Swaps.add(new DescentSolver.Swap(block.machine,block.firstTask,block.lastTask));
        }
        else {
            Swaps.add(new DescentSolver.Swap(block.machine, block.firstTask,block.firstTask+1 ));
            Swaps.add(new DescentSolver.Swap(block.machine,block.lastTask-1, block.lastTask));
        }
        return Swaps;
    }
}
