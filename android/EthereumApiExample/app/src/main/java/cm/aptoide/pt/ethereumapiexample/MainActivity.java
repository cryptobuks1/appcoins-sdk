package cm.aptoide.pt.ethereumapiexample;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import cm.aptoide.pt.EthereumApi;
import cm.aptoide.pt.EthereumApiFactory;
import cm.aptoide.pt.erc20.Erc20Transfer;
import cm.aptoide.pt.ws.etherscan.BalanceResponse;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

  private static final String CONTRACT_ADDRESS = "8dbf4349cbeca08a02cc6b5b0862f9dd42c585b9";
  private static final String RECEIVER_ADDR = "62a5c1680554A61334F5c6f6D7dA6044b6AFbFe8";
  private static final String MAIN_PREFS = "MainPrefs";

  private EthereumApi ethereumApi;
  private EtherAccountManager etherAccountManager;

  private TextView balanceTextView;
  private TextView yourId;
  private TextView addressTextView;
  private TextView amountTextView;

  private ScheduledExecutorService scheduledExecutorService;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    scheduledExecutorService = Executors.newScheduledThreadPool(1);

    assignViews();

    scheduledExecutorService.schedule(new Runnable() {
      @Override public void run() {
        ethereumApi = EthereumApiFactory.createEthereumApi();

        etherAccountManager =
            new EtherAccountManager(ethereumApi, getSharedPreferences(MAIN_PREFS, MODE_PRIVATE));

        runOnUiThread(new Runnable() {
          @Override public void run() {
            yourId.setText(Hex.toHexString(etherAccountManager.getECKey()
                .getAddress()));
          }
        });
      }
    }, 0, TimeUnit.SECONDS);

    scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
      @Override public void run() {
        ethereumApi.getTokenBalance(CONTRACT_ADDRESS, Hex.toHexString(etherAccountManager.getECKey()
            .getAddress()))
            .subscribeOn(Schedulers.io())
            .subscribe(new Action1<BalanceResponse>() {
              @Override public void call(BalanceResponse balanceResponse) {
                runOnUiThread(new Runnable() {
                  @Override public void run() {
                    Toast.makeText(MainActivity.this, "Refreshing data", Toast.LENGTH_SHORT)
                        .show();
                  }
                });

                BigDecimal resultBigDecimal = new BigDecimal(balanceResponse.result);

                setBalance(balanceTextView,
                    resultBigDecimal.divide(new BigDecimal("100"), MathContext.DECIMAL32)
                        .toString());
              }
            });
      }
    }, 0, 5, TimeUnit.SECONDS);
  }

  private void setBalance(final TextView balanceTextView, final String result) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        balanceTextView.setText(result);
      }
    });
  }

  private void assignViews() {
    balanceTextView = findViewById(R.id.balanceTextView);
    yourId = findViewById(R.id.your_id);
    addressTextView = findViewById(R.id.address_text_view);
    amountTextView = findViewById(R.id.amount);
  }

  public void paySomething(View v) {
    PaySomethingFragment.newInstance(this, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int i) {
        new Thread(new Runnable() {
          @Override public void run() {
            etherAccountManager.getCurrentNonce()
                .flatMap(new Func1<Long, Observable<?>>() {
                  @Override public Observable<Object> call(Long nonce) {
                    return ethereumApi.call(nonce.intValue(), CONTRACT_ADDRESS,
                        new Erc20Transfer(RECEIVER_ADDR, 1), etherAccountManager.getECKey());
                  }
                })
                //ethereumApi.call(nonce, CONTRACT_ADDRESS, erc20Transfer, etherAccountManager.getECKey())
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Object>() {
                  @Override public void call(Object o) {
                    System.out.println(o);
                  }
                }, new Action1<Throwable>() {
                  @Override public void call(final Throwable throwable) {
                    runOnUiThread(new Runnable() {
                      @Override public void run() {
                        Toast.makeText(MainActivity.this, throwable.getMessage(),
                            Toast.LENGTH_SHORT)
                            .show();
                      }
                    });
                  }
                });
          }
        }).start();
      }
    })
        .show(getSupportFragmentManager(), "MyDialog");
  }

  public void sendTokens(final View view) {
    if (validateInputs((TextView) view, amountTextView)) {
      etherAccountManager.getCurrentNonce()
          .flatMap(new Func1<Long, Observable<?>>() {
            @Override public Observable<Object> call(Long nonce) {
              String receiverAddr = addressTextView.getText()
                  .toString();
              return ethereumApi.call(nonce.intValue(), CONTRACT_ADDRESS,
                  new Erc20Transfer(receiverAddr.substring(2), Integer.parseInt(
                      amountTextView.getText()
                          .toString())), etherAccountManager.getECKey());
            }
          })
          .subscribeOn(Schedulers.io())
          .doOnSubscribe(new Action0() {
            @Override public void call() {
              runOnUiThread(new Runnable() {
                @Override public void run() {
                  Toast.makeText(MainActivity.this, "Sending transaction", Toast.LENGTH_SHORT)
                      .show();
                  amountTextView.setText("");
                }
              });
            }
          })
          .subscribe(new Action1<Object>() {
            @Override public void call(Object o) {
            }
          }, new Action1<Throwable>() {
            @Override public void call(Throwable throwable) {
              throwable.printStackTrace();
            }
          });
    } else {
      runOnUiThread(new Runnable() {
        @Override public void run() {
          Toast.makeText(MainActivity.this, "Address and Amount required!", Toast.LENGTH_SHORT)
              .show();
        }
      });
    }
  }

  private boolean validateInputs(TextView addressTextView, TextView amountTextView) {
    return !StringUtils.isEmpty(addressTextView.getText()) && !StringUtils.isEmpty(
        amountTextView.getText());
  }
}
