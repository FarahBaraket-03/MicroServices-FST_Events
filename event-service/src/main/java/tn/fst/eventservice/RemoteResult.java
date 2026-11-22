package tn.fst.eventservice;

import java.util.List;

/** Empty data accompanied by available=false must never be treated as real zero data. */
public record RemoteResult<T>(List<T> data, boolean available, String warning) {
  public static <T> RemoteResult<T> ok(List<T> data) {
    return new RemoteResult<>(List.copyOf(data), true, "");
  }

  public static <T> RemoteResult<T> unavailable(String dependency) {
    return new RemoteResult<>(
        List.of(), false, dependency + " unavailable or returned invalid data");
  }
}
