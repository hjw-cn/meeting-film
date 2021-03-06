package com.stylefeng.guns.rest.modular.film;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.rpc.RpcContext;
import com.stylefeng.guns.api.film.FilmAsyncServiceAPI;
import com.stylefeng.guns.api.film.FilmServiceAPI;
import com.stylefeng.guns.api.film.vo.*;
import com.stylefeng.guns.rest.modular.film.vo.FilmConditionVO;
import com.stylefeng.guns.rest.modular.film.vo.FilmIndexVO;
import com.stylefeng.guns.rest.modular.film.vo.FilmRequestVO;
import com.stylefeng.guns.rest.modular.vo.ResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 佛祖保佑    永无BUG
 *
 * @author Jerry
 **/

@RestController
@RequestMapping("/film/")
public class FilmController {

    private static final String IMG_PRE = "http://img.meetingshop.cn/";

    @Reference(interfaceClass = FilmServiceAPI.class, check = false)
    private FilmServiceAPI filmServiceAPI;

    // 异步的serviceApi方法接口
    @Reference(interfaceClass = FilmAsyncServiceAPI.class, async = true, check = false)
    private FilmAsyncServiceAPI filmAsyncServiceAPI;

    // 获取首页信息接口
    /*
    API网关：
        1.接口功能聚合（API聚合）
            好处：1. 六个接口一次请求
                 2. 同一个接口对外暴露，降低前后端分离开发的复杂度
     */
    @RequestMapping(value = "getIndex", method = RequestMethod.GET)
    public ResponseVO getIndex() {


        FilmIndexVO filmIndexVO = new FilmIndexVO();
        // 获取banner信息
        filmIndexVO.setBanners(filmServiceAPI.getBanners());
        // 获取正在热映的电影
        filmIndexVO.setHotFilms(filmServiceAPI.getHotFilms(true, 8, 1, 1, 99, 99, 99));
        // 即将上映的影片
        filmIndexVO.setSoonFilms(filmServiceAPI.getSoonFilms(true, 8, 1, 1, 99, 99, 99));
        // 票房排行信息
        filmIndexVO.setBoxRanking(filmServiceAPI.getBoxRanking());
        // 人气排行信息
        filmIndexVO.setExceptRanking(filmServiceAPI.getExceptRanking());
        // top100
        filmIndexVO.setTop100(filmServiceAPI.getTop());

        return ResponseVO.success(IMG_PRE, filmIndexVO);
    }


    @RequestMapping(value = "getConditionList", method = RequestMethod.GET)
    public ResponseVO getConditionList(@RequestParam(name = "catId", required = false, defaultValue = "99") String catId,
                                       @RequestParam(name = "sourceId", required = false, defaultValue = "99") String sourceId,
                                       @RequestParam(name = "yearId", required = false, defaultValue = "99") String yearId) {


        FilmConditionVO filmConditionVO = new FilmConditionVO();
        // 标识位
        boolean flag = false;
        // 类型集合
        List<CatVO> cats = filmServiceAPI.getCats();
        System.out.println(cats);
        List<CatVO> catResult = new ArrayList<>();
        CatVO cat = null;
        for (CatVO catVO : cats) {
            if (catVO.getCatId().equals("99")) {
                cat = catVO;
                continue;
            }
            // 判断集合是否存在catId，如果存在 则将对应的实体变成Active状态
            if (catVO.getCatId().equals(catId)) {
                flag = true;
                catVO.setActive(true);
            } else {
                catVO.setActive(false);
            }
            catResult.add(catVO);
        }
        // 如果不存在 则默认将全部变为Active状态
        if (!flag) {
            cat.setActive(true);
            catResult.add(cat);
        } else {
            cat.setActive(false);
            catResult.add(cat);
        }

        // 片源集合
        flag = false;
        List<SourceVO> sources = filmServiceAPI.getSources();
        List<SourceVO> sourceResult = new ArrayList<>();
        SourceVO source = null;
        for (SourceVO sourceVO : sources) {
            // 如果存在sourceId
            if (sourceVO.getSourceId().equals("99")) {
                source = sourceVO;
                continue;
            }
            // 判断集合是否存在catId，如果存在 则将对应的实体变成Active状态
            if (sourceVO.getSourceId().equals(sourceId)) {
                flag = true;
                sourceVO.setActive(true);
            } else {
                sourceVO.setActive(false);
            }
            sourceResult.add(sourceVO);
        }
        // 如果不存在 则默认将全部变为Active状态
        if (!flag) {
            source.setActive(true);
            sourceResult.add(source);
        } else {
            source.setActive(false);
            sourceResult.add(source);
        }

        // 年份集合
        flag = false;
        List<YearVO> years = filmServiceAPI.getYears();
        List<YearVO> yearResult = new ArrayList<>();
        YearVO year = null;
        for (YearVO yearVO : years) {
            // 如果存在sourceId
            if (yearVO.getYearId().equals("99")) {
                year = yearVO;
                continue;
            }
            // 判断集合是否存在catId，如果存在 则将对应的实体变成Active状态
            if (yearVO.getYearId().equals(yearId)) {
                flag = true;
                yearVO.setActive(true);
            } else {
                yearVO.setActive(false);
            }
            yearResult.add(yearVO);
        }
        // 如果不存在 则默认将全部变为Active状态
        if (!flag) {
            year.setActive(true);
            yearResult.add(year);
        } else {
            year.setActive(false);
            yearResult.add(year);
        }


        filmConditionVO.setCatInfo(catResult);
        filmConditionVO.setSourceInfo(sourceResult);
        filmConditionVO.setYearInfo(yearResult);
        return ResponseVO.success(filmConditionVO);
    }

    @RequestMapping(value = "getFilms", method = RequestMethod.GET)
    public ResponseVO getFilms(FilmRequestVO filmRequestVO) {

        FilmVO filmVO = null;
        // 根据showType判断影片查询类型
        // 1-正在热映，2-即将上映，3-经典影片
        switch (filmRequestVO.getShowType()) {
            case 1:
                filmVO = filmServiceAPI.getHotFilms(false, filmRequestVO.getPageSize(),
                        filmRequestVO.getNowPage(), filmRequestVO.getSortId(),
                        filmRequestVO.getSourceId(), filmRequestVO.getYearId(),
                        filmRequestVO.getCatId());
                break;
            case 2:
                filmVO = filmServiceAPI.getSoonFilms(false, filmRequestVO.getPageSize(),
                        filmRequestVO.getNowPage(), filmRequestVO.getSortId(),
                        filmRequestVO.getSourceId(), filmRequestVO.getYearId(),
                        filmRequestVO.getCatId());
                break;
            case 3:
                filmVO = filmServiceAPI.getClassicFilms(filmRequestVO.getPageSize(),
                        filmRequestVO.getNowPage(), filmRequestVO.getSortId(),
                        filmRequestVO.getSourceId(), filmRequestVO.getYearId(),
                        filmRequestVO.getCatId());
                break;
            default:
                filmVO = filmServiceAPI.getHotFilms(false, filmRequestVO.getPageSize(),
                        filmRequestVO.getNowPage(), filmRequestVO.getSortId(),
                        filmRequestVO.getSourceId(), filmRequestVO.getYearId(),
                        filmRequestVO.getCatId());
                break;
        }
        // 根据sortId排序
        // 添加各种条件查询
        // 判断当前是第几页

        return ResponseVO.success(filmVO.getNowPage(), filmVO.getTotalPage(), IMG_PRE, filmVO.getFilmInfo());
    }


    @RequestMapping(value = "films/{searchParam}", method = RequestMethod.GET)
    public ResponseVO films(@PathVariable("searchParam") String searchParam, int searchType) throws ExecutionException, InterruptedException {
        //根据searchType判断查找类型
        FilmDetailVO filmDetail = filmServiceAPI.getFilmDetail(searchType, searchParam);
        if (filmDetail == null) {
            return ResponseVO.serviceFail("没有可查询的影片");
        } else if (filmDetail.getFilmId() == null || filmDetail.getFilmId().trim().length() == 0) {
            return ResponseVO.serviceFail("没有可查询的影片");
        }

        String filmId = filmDetail.getFilmId();
        // 不同的查找类型，传入的条件会有所不同
        // 查询影片的详细信息  --->  Dubbo的异步调用
        // 获取影片描述信息
        filmAsyncServiceAPI.getFilmDesc(filmId);
        Future<FilmDescVO> filmDescVOFuture = RpcContext.getContext().getFuture();

        // 获取图片信息
        filmAsyncServiceAPI.getImgs(filmId);
        Future<ImgVO> imgVOFuture = RpcContext.getContext().getFuture();

        // 获取导演信息
        filmAsyncServiceAPI.getDirectorInfo(filmId);
        Future<ActorVO> directorVOFuture = RpcContext.getContext().getFuture();

        // 获取演员信息
        filmAsyncServiceAPI.getActors(filmId);
        Future<List<ActorVO>> actorsVOFuture = RpcContext.getContext().getFuture();

        // 组织Actor属性
        ActorRequestVO actorRequestVO = new ActorRequestVO();
        actorRequestVO.setActors(actorsVOFuture.get());
        actorRequestVO.setDirector(directorVOFuture.get());

        // 组织info对象
        InfoRequestVO infoRequestVO = new InfoRequestVO();
        infoRequestVO.setActors(actorRequestVO);
        infoRequestVO.setBiography(filmDescVOFuture.get().getBiography());
        infoRequestVO.setFilmId(filmId);
        infoRequestVO.setImgs(imgVOFuture.get());

        // 组织成为返回值对象
        filmDetail.setInfo04(infoRequestVO);
        return ResponseVO.success(IMG_PRE, filmDetail);
    }
}