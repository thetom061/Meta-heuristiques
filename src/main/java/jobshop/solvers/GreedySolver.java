package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Schedule;
import jobshop.encodings.Task;

import java.lang.reflect.Array;
import java.util.*;

/** An empty shell to implement a greedy solver. */
public class GreedySolver implements Solver {

    /** All possible priorities for the greedy solver. */
    public enum Priority {
        SPT, LPT, SRPT, LRPT, EST_SPT, EST_LPT, EST_SRPT, EST_LRPT
    }

    /** Priority that the solver should use. */
    final Priority priority;

    /** Creates a new greedy solver that will use the given priority. */
    public GreedySolver(Priority p) {
        this.priority = p;
    }

    public Task SPT(List<Task> list,Instance instance){
        Task task=list.get(0);
        for (Task atask : list){
            if (instance.duration(atask)<instance.duration(task)){
                task=atask;
            }
        }
        return task;
    }

    public Task LRPT(List<Task> list, Instance instance){
        Task task= list.get(0);
        int bestduration=0;
        for (Task atask : list) {
            int duration=0;
            for (int i = atask.task; i < instance.numTasks; i++) {
                duration += instance.duration(atask.job, i);
            }
            if (duration>bestduration){
                bestduration=duration;
                task=atask;
            }
        }
        return task;
    }


    //retourne liste des tâches pouvant commencer le plus rapidement
    public List<Task> EST(List<Task> list, Instance instance,int[] tempsmach,int[] tempsjob){
        //On trouve la durée de démarrage la plus courte
        int shortest=Integer.MAX_VALUE;
        for (Task atask: list){
            int time= Math.max(tempsmach[instance.machine(atask)], tempsjob[atask.job]);
            if (time<shortest){
                shortest=time;
            }
        }
        //On met tout ceux qui ont une durée égale à cette durée la plus courte dans la liste
        List<Task> alist=new ArrayList<>();
        for (Task atask: list){
            if (Math.max(tempsmach[instance.machine(atask)], tempsjob[atask.job])==shortest){
                alist.add(atask);
            }
        }
        return alist;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        long time=System.currentTimeMillis();
        ResourceOrder greedyOrder = new ResourceOrder(instance);
        int numJobs= instance.numJobs;
        int numMachines=instance.numMachines;

        //uniquement utilisé si on est est en EST
        int[] tempsmach = new int[numMachines];
        int[] tempsjob = new int[numJobs];
        for (int i = 0; i < numMachines; i++) {
            tempsmach[i] = 0;
        }
        for (int i = 0; i < numJobs; i++) {
            tempsjob[i] = 0;
        }

        //initialisation des tâches réalisables

        List<Task> taches=new ArrayList<>();

        //on mets les taches réalisables
        for (int j=0;j<numJobs;j++) {
            taches.add(new Task(j,0));
        }
        while (!taches.isEmpty()) {
            //si on timeout
            if (System.currentTimeMillis()-time>deadline){
                Result result= new Result(instance,greedyOrder.toSchedule(),Result.ExitCause.Timeout);
                return result;
            }
            Task addTask=null;

            if (priority == Priority.SPT){
                addTask=SPT(taches,instance);
            }
            if (priority==Priority.LRPT){
                addTask=LRPT(taches,instance);
            }
            if (priority==Priority.EST_SPT){
                addTask=SPT(EST(taches,instance,tempsmach,tempsjob),instance);
            }
            if (priority==Priority.EST_LRPT){
                addTask=LRPT(EST(taches,instance,tempsmach,tempsjob),instance);
            }
            //utilisé uniquement dans les priorité avec EST
            //on update le temps machine et le temps job
            //le prochain temps d'utilisation est le maximum entre temps mach et temps job + la durée de la tâche
            int temps=Math.max(tempsmach[instance.machine(addTask)]+instance.duration(addTask),tempsjob[addTask.job]+instance.duration(addTask));
            tempsmach[instance.machine(addTask)]=temps;
            tempsjob[addTask.job]=temps;

            //On rajoute dans le ressourceorder
            greedyOrder.addTaskToMachine(instance.machine(addTask),addTask);

            //On enlève la tâche ajouté dans le ressource order des tâches réalisable et on ajoute la suivante de son job si elle existe
            taches.remove(addTask);
            if (numMachines>addTask.task+1) {
                taches.add(new Task(addTask.job, addTask.task + 1));
            }
        }
        
        Result result= new Result(instance,greedyOrder.toSchedule(),Result.ExitCause.Blocked);

        return result;
    }
}
