package org.example;

/**
 * Hello world!
 *
 */
class A {
    int emp_id;
    String email;

    A(int emp_id, String email) {
        this.emp_id = emp_id;
        this.email = email;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        A obj2 = (A) obj;
        if (this.email == null && obj2.email == null) {
            return true;
        }
        if (this.email == null || obj2.email == null) {
            return false;
        }

        if (this.email.length() != obj2.email.length()) {
            return false;
        }

//        for (int i = 0; i < this.email.length(); i++) {
//            if (this.email.charAt(i) != obj2.email.charAt(i)) {
//                return false;
//            }
//        }
        return this.email.equals(obj2.email);
    }

    public static void main(String[] args){
        A obj1 = new A(1, "jeya@gmail.com");
        A obj2 = new A(2, "jeyashree@gmail.com");
        System.out.println(obj1.equals(obj2));
    }
}
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
    }
}
