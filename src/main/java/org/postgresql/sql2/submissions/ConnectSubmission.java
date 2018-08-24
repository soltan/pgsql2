package org.postgresql.sql2.submissions;

import org.postgresql.sql2.communication.NetworkConnect;
import org.postgresql.sql2.communication.network.NetworkConnectRequest;
import org.postgresql.sql2.communication.packets.DataRow;
import org.postgresql.sql2.operations.helpers.ParameterHolder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class ConnectSubmission implements org.postgresql.sql2.PGSubmission<Void> {

  final private Supplier<Boolean> cancel;
  private CompletableFuture<Void> publicStage;
  private final AtomicBoolean sendConsumed = new AtomicBoolean(false);
  private Types completionType;

  private Collector collector;
  private Object collectorHolder;
  private Consumer<Throwable> errorHandler;

  private GroupSubmission groupSubmission;
  
  private NetworkConnectRequest request;

  public ConnectSubmission(Supplier<Boolean> cancel, Types completionType, Consumer<Throwable> errorHandler,
      GroupSubmission groupSubmission) {
    this.cancel = cancel;
    this.completionType = completionType;
    this.errorHandler = errorHandler;
    this.groupSubmission = groupSubmission;
    this.request = new NetworkConnectRequest(this);

    if (groupSubmission != null) {
      groupSubmission.stackFuture((CompletableFuture<Void>) getCompletionStage());
    }
  }
  
  public NetworkConnect getNetworkConnect() {
    return this.request;
  }

  @Override
  public CompletionStage<Boolean> cancel() {
    return new CompletableFuture<Boolean>().completeAsync(cancel);
  }

  @Override
  public CompletionStage<Void> getCompletionStage() {
    if (publicStage == null)
      publicStage = new CompletableFuture<>();
    return publicStage;
  }

  @Override
  public String getSql() {
    return null;
  }

  @Override
  public AtomicBoolean getSendConsumed() {
    return sendConsumed;
  }

  @Override
  public ParameterHolder getHolder() {
    return null;
  }

  @Override
  public Types getCompletionType() {
    return completionType;
  }

  @Override
  public void setCollector(Collector collector) {
    this.collector = collector;

    collectorHolder = collector.supplier().get();
  }

  @Override
  public Object finish(Object finishObject) {
    ((CompletableFuture<Void>) getCompletionStage()).complete(null);
    return null;
  }

  @Override
  public void addRow(DataRow row) {
    try {
      collector.accumulator().accept(collectorHolder, row);
    } catch (Throwable e) {
      publicStage.completeExceptionally(e);
    }
  }

  @Override
  public List<Integer> getParamTypes() throws ExecutionException, InterruptedException {
    return null;
  }

  @Override
  public int numberOfQueryRepetitions() throws ExecutionException, InterruptedException {
    return 1;
  }

  @Override
  public Consumer<Throwable> getErrorHandler() {
    return errorHandler;
  }

}
