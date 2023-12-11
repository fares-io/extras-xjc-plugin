package test.time;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.OffsetDateTime;

public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {

  @Override
  public OffsetDateTime unmarshal(String v) {
    if (v == null) return null;
    return OffsetDateTime.parse(v);
  }

  @Override
  public String marshal(OffsetDateTime v) {
    if (v == null) return null;
    return v.toString();
  }

}
