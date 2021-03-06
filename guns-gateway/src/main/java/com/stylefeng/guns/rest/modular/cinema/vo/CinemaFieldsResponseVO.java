package com.stylefeng.guns.rest.modular.cinema.vo;

import com.stylefeng.guns.api.cinema.vo.CinemaInfoVO;
import com.stylefeng.guns.api.cinema.vo.FilmInfoVO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Jerry
 **/
@Data
public class CinemaFieldsResponseVO implements Serializable {
    private CinemaInfoVO cinemaInfo;
    private List<FilmInfoVO> filmList;
}
