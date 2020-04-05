package test.time;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;

public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

  @Override
  public LocalDateTime unmarshal(String v) {
    if (v == null) return null;
    return LocalDateTime.parse(v);
  }

  @Override
  public String marshal(LocalDateTime v) {
    if (v == null) return null;
    return v.toString();
  }

}
