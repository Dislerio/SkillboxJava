import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.junit.Before;

public class BankTest extends TestCase {

    private HashMap<String, Account> accounts;
    private ArrayList<String> keySet;
    private Bank bank;

    public BankTest() {
    }

    private void transferRandomAmount(int from, int to) {
        String fromAccountNumber = bank.getAccounts().get(keySet.get(from)).getAccNumber();
        String toAccountNumber = bank.getAccounts().get(keySet.get(to)).getAccNumber();
        try {
            long amount = Math.round((Math.random() * bank.getBalance(fromAccountNumber)) / 100);

            /*
            System.out.println(Thread.currentThread().getName() + " From account balance: " + bank
                .getBalance(fromAccountNumber));
            System.out.println(Thread.currentThread().getName() + " To account balance: " + bank
                .getBalance(toAccountNumber));

             */
            bank.transfer(fromAccountNumber, toAccountNumber, amount);

            /*System.out.println(Thread.currentThread().getName() +
                "Transfering " + amount + " from " + fromAccountNumber + " to " + toAccountNumber);
            System.out.println(Thread.currentThread().getName() + " From account balance: " + bank
                .getBalance(fromAccountNumber));
            System.out.println(Thread.currentThread().getName() + " To account balance: " + bank
                .getBalance(toAccountNumber));
            System.out.println("----------------------------------");

             */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long checkMoneySum() {
        long sum = 0;

        for (int i = 0; i < keySet.size(); i++) {
            try {
                sum += bank.getBalance(keySet.get(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sum;
    }


    @Before
    public void setUp() throws InterruptedException {
        FraudControl mockFraudControl = mock(FraudControl.class);
        when(mockFraudControl.isFraud(anyString(), anyString(), anyLong())).thenReturn(false);

        accounts = new HashMap<>();
        bank = new Bank(accounts, mockFraudControl);
        keySet = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Account account = new Account();
            accounts.put(account.getAccNumber(), account);
        }
        keySet.addAll(accounts.keySet());
    }

    public void test_transfer_under_load() throws InterruptedException {
        long startSum = checkMoneySum();
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        for (int l = 0; l < 1000; l++) {
            executorService.submit(() -> {
                try {
                    for (int i = 0; i < 500; ++i) {
                        for (int j = 0; j < keySet.size() - 1; j++) {
                            int giver = (int) Math.round(keySet.size() * Math.random());
                            if (giver >= keySet.size()) {
                                giver -= 1;
                            }
                            int receiver = (int) Math.round(keySet.size() * Math.random());
                            if (receiver >= keySet.size()) {
                                receiver -= 1;
                            }
                            transferRandomAmount(giver, receiver);
                            //System.err.println(Thread.currentThread().getName());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        long endSum = checkMoneySum();
        //System.out.println("Start sum is " + startSum + ", end sum is " + endSum);
        assertEquals(startSum, endSum);

    }
}

