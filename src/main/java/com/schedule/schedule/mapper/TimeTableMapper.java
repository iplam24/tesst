package com.schedule.schedule.mapper;

import com.schedule.schedule.dto.TimeTableEntryDTO;
import com.schedule.schedule.entity.TimeTableEntry;

public class TimeTableMapper {

    public static TimeTableEntryDTO toDTO(TimeTableEntry e) {
        TimeTableEntryDTO dto = new TimeTableEntryDTO();
        dto.setId(e.getId());
        dto.setMssv(e.getMssv());
        dto.setMaMon(e.getMaMon());
        dto.setTenMon(e.getTenMon());
        dto.setNhom(e.getNhom());
        dto.setToNhom(e.getToNhom());
        dto.setSoTinChi(e.getSoTinChi());
        dto.setSoTietLT(e.getSoTietLT());
        dto.setSoTietTH(e.getSoTietTH());
        dto.setLop(e.getLop());
        dto.setThu(e.getThu());
        dto.setTietBatDau(e.getTietBatDau());
        dto.setSoTiet(e.getSoTiet());
        dto.setPhong(e.getPhong());
        dto.setGiangVien(e.getGiangVien());
        dto.setChuoiTuan(e.getChuoiTuan());
        dto.setLastUpdated(e.getLastUpdated());
        return dto;
    }
}
