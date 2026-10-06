package ar.edu.utn.frc.tup.piii.dtos.response;

import java.util.List;

/** Response DTO for paginated card list ({data: [], total, page, size}). */
public class CardPageResponse {
    private List<CardResponse> data;
    private long total;
    private int page;
    private int size;

    public List<CardResponse> getData() { return data; }
    public void setData(List<CardResponse> data) { this.data = data; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
