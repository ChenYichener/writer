class Kafka {

    public static void main(String[] args) {
        
        ReplicaManager replicaManager =   new ReplicaManager();
        replicaManager.loadReplicasFromDisk();
    }
}