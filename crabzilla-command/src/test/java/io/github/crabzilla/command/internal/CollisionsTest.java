package io.github.crabzilla.command.internal;//package io.github.crabzilla.stack;

//public class CollisionsTest {
//    private final static int num_of_users = 10000 * 1000;
//    private final static int num_of_stacks = 50;
//
//    public static void main(String[] args) {
//        int collisions = 0;
//        for (int i = 0; i < num_of_users; i++) {
//            collisions += calcCollisionsForUser();
//        }
//        System.out.println("Had " + collisions + " collisions for " + num_of_users + " users");
//    }
//
//    private static int calcCollisionsForUser() {
//        int collisions = 0;
//        java.util.Set<Integer> uuidSet = new java.util.HashSet<Integer>(num_of_stacks * 2);
//        for (int i = 0; i < num_of_stacks; i++) {
//            String uuid = java.util.UUID.randomUUID().toString();
//            Integer uuidHashcode = uuid.hashCode();
//            if (uuidSet.contains(uuidHashcode)) {
//                collisions++;
//            }
//            uuidSet.add(uuidHashcode);
//        }
//        return collisions;
//    }
//}
