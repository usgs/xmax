package com.isti.traceview.filters;

import static asl.utils.timeseries.TimeSeriesUtils.getFirstTimeSeries;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class FilterHPTest {

  @Test
  public void basicFilterTest() throws Exception {

    //Expected results generated using scipy's butterworth Bandpass filter
    Map<Integer, Double> expected = new HashMap<Integer, Double>() {{
      put(0, -657.917681964087);
      put(11520, -6.929632072408481);
      put(23040, -1.0576984524930424);
      put(34560, -5.210569836877511);
      put(46080, 6.704468994167286);
      put(57600, -9.83224991261551);
      put(69120, -1.1213819486490308);
      put(80640, 2.2656693348639294);
      put(92160, 5.605620553377435);
      put(103680, 0.9245998527103438);
      put(115200, 4.106781008215421);
      put(126720, 3.2652109295058693);
      put(138240, -1.0962419813947122);
      put(149760, -2.9399996476488255);
      put(161280, -0.825267453193419);
      put(172800, -5.024900563799065);
      put(184320, -12.363314266764633);
      put(195840, -0.3841363567753433);
      put(207360, -5.785474961126113);
      put(218880, 4.477871267821399);
      put(230400, -2.54942028762747);
      put(241920, -5.8999885925246645);
      put(253440, 5.941869622256036);
      put(264960, 3.597094379654891);
      put(276480, -3.6956948513348493);
      put(288000, 5.741450287515704);
      put(299520, -1.174218867053213);
      put(311040, 4.301883248306012);
      put(322560, -3.5882524795668616);
      put(334080, -3.1780620689008288);
      put(345600, -0.4957030441717052);
      put(357120, 8.554516132175708);
      put(368640, 1.676500830208056);
      put(380160, -2.245416503846357);
      put(391680, 5.502057804947981);
      put(403200, 5.372408885312474);
      put(414720, -1.5173362644248414);
      put(426240, -3.9656184509728973);
      put(437760, -6.123536306190601);
      put(449280, -0.1701240701529514);
      put(460800, 5.068194529414086);
      put(472320, -2.4220012916391056);
      put(483840, 3.892018585450728);
      put(495360, -2.1359611734087594);
      put(506880, 0.7366011810963755);
      put(518400, -2.268624307448789);
      put(529920, -3.473491778745398);
      put(541440, 1.08269657402559);
      put(552960, 1.3390044341802536);
      put(564480, -6.159389517179534);
      put(576000, -1.8194310578107888);
      put(587520, -12.36765755727788);
      put(599040, 3.1677385158676543);
      put(610560, 12.054399063878861);
      put(622080, -1.035120336034879);
      put(633600, -8.011931468025466);
      put(645120, -7.3962962882926835);
      put(656640, -1.8715643683597705);
      put(668160, -4.480819308392029);
      put(679680, 7.049190215894896);
      put(691200, 5.387440544218464);
      put(702720, 0.18227712873022028);
      put(714240, -6.656850597645985);
      put(725760, -9.31008971320091);
      put(737280, -3.87987195513125);
      put(748800, 3.7798211605628467);
      put(760320, 0.9340694587585858);
      put(771840, -5.842030740329847);
      put(783360, 0.2708210300338578);
      put(794880, 4.734305362937093);
      put(806400, -0.971543094225467);
      put(817920, -4.777868038067879);
      put(829440, -0.0010401202277421362);
      put(840960, -3.1489227419624513);
      put(852480, 2.567753646209951);
      put(864000, 0.5152133078659062);
      put(875520, 0.6946348937856044);
      put(887040, 3.2834706322378224);
      put(898560, -0.2523176529693387);
      put(910080, -0.3555188621793661);
      put(921600, 4.269290988207381);
      put(933120, 4.717772525539942);
      put(944640, -0.45143386465497315);
      put(956160, -7.536684207153428);
      put(967680, 0.04693708755710446);
      put(979200, -5.840171045905265);
      put(990720, 1.874759247452289);
      put(1002240, -2.324575736084398);
      put(1013760, -2.753145932187863);
      put(1025280, -0.8390079079565567);
      put(1036800, 4.038363666123473);
      put(1048320, 6.533307812428916);
      put(1059840, -6.915164536533666);
      put(1071360, -1.5077734949965134);
      put(1082880, 3.9114762311517666);
      put(1094400, -3.4577206529436353);
      put(1105920, 3.0503469404858947);
      put(1117440, 6.855664709771304);
      put(1128960, -3.012645246335694);
      put(1140480, -1.1595769801014058);
      put(1152000, -7.346652109437684);
      put(1163520, 10.319964442133767);
      put(1175040, -1.5264902474522728);
      put(1186560, 0.05869256303162729);
      put(1198080, -0.5977253427033418);
      put(1209600, 2.637086264130204);
      put(1221120, -3.9944257957507716);
      put(1232640, -0.3670135098561218);
      put(1244160, 0.8280636535805002);
      put(1255680, 4.328661250114578);
      put(1267200, 2.840517093768014);
      put(1278720, -2.9300412672313882);
      put(1290240, 3.405352576562855);
      put(1301760, 9.925645679819922);
      put(1313280, -2.513978866771083);
      put(1324800, 2.541006817807329);
      put(1336320, -0.8511920789987357);
      put(1347840, 1.2331549539856042);
      put(1359360, -7.620298138918599);
      put(1370880, -0.08265605632320217);
      put(1382400, -2.3945020957745555);
      put(1393920, 0.6870646001447085);
      put(1405440, 0.6554942218824067);
      put(1416960, 4.125407737147583);
      put(1428480, -3.345469553745488);
      put(1440000, -3.173474370511542);
      put(1451520, -0.9738376966811302);
      put(1463040, 5.350963919528439);
      put(1474560, -4.431948218620789);
      put(1486080, -1.8332947378107178);
      put(1497600, -5.824364034578309);
      put(1509120, 1.6447418607337454);
      put(1520640, 5.596484830630175);
      put(1532160, 1.1313364158460644);
      put(1543680, -5.496097152882413);
      put(1555200, -0.0948475433060878);
      put(1566720, -2.6618063074488134);
      put(1578240, 7.769367793627509);
      put(1589760, 0.8899838776703746);
      put(1601280, 0.18995570015158592);
      put(1612800, 2.042976742638814);
      put(1624320, -3.796390861995633);
      put(1635840, 1.2976112858845568);
      put(1647360, 4.249706507875658);
      put(1658880, 1.3930031297913956);
      put(1670400, -0.28195657598865864);
      put(1681920, 4.0151203789008605);
      put(1693440, -8.79759823053871);
      put(1704960, -1.7492249094096621);
      put(1716480, -5.063290504351329);
      put(1728000, -2.1150163481270283);
      put(1739520, 5.191773526032534);
      put(1751040, -1.2120030195843015);
      put(1762560, -10.6367309493358);
      put(1774080, 6.616173373661184);
      put(1785600, -5.488006895764158);
      put(1797120, -3.6902058759250167);
      put(1808640, 8.55569096697458);
      put(1820160, 4.1892778607677315);
      put(1831680, -5.080635488626598);
      put(1843200, -0.8181500671613833);
      put(1854720, -0.5984389351118153);
      put(1866240, 4.337510352385749);
      put(1877760, -5.804197658967212);
      put(1889280, 2.2740784311809534);
      put(1900800, 3.3495559546118443);
      put(1912320, 9.838077983007906);
      put(1923840, 0.5951017940759584);
      put(1935360, 0.4804285645681716);
      put(1946880, 11.82688046533437);
      put(1958400, -6.5944932797667235);
      put(1969920, 1.9942222365542648);
      put(1981440, -7.339584184055212);
      put(1992960, -10.889515435833118);
      put(2004480, -0.5485158095611098);
      put(2016000, 1.6958423167873775);
      put(2027520, 4.611467842627633);
      put(2039040, 2.5687876050399723);
      put(2050560, 2.771314857633598);
      put(2062080, 1.4917203209783452);
      put(2073600, 10.412631666164145);
      put(2085120, 11.417004875828297);
      put(2096640, -1.7998391176046766);
      put(2108160, 1.5978622394533488);
      put(2119680, 0.9600731663445004);
      put(2131200, 1.8060600215351883);
      put(2142720, -3.3084767266253436);
      put(2154240, -1.3954721216397843);
      put(2165760, 2.493256691205829);
      put(2177280, 1.0351721316337716);
      put(2188800, -3.560075222402773);
      put(2200320, 1.3051727620807014);
      put(2211840, 11.314082803921167);
      put(2223360, -5.320367719325247);
      put(2234880, -2.0265423678313823);
      put(2246400, -4.723828229682454);
      put(2257920, 8.122176429406494);
      put(2269440, 9.935119116698559);
      put(2280960, -0.895764346770477);
      put(2292480, 4.704070316187028);
      put(2304000, 1.1329781306549194);
      put(2315520, 12.32859989223715);
      put(2327040, -1.4360449023151318);
      put(2338560, -1.4972805933042252);
      put(2350080, -3.056898977522394);
      put(2361600, -2.241109454424816);
      put(2373120, -5.854232931339141);
      put(2384640, -7.0340737400784406);
      put(2396160, 6.5642659206928045);
      put(2407680, 2.348897006212354);
      put(2419200, 106.04872962735732);
      put(2430720, 12.891981586582489);
      put(2442240, 11.561888986491567);
      put(2453760, -4.189904578176424);
      put(2465280, 5.805105741490195);
      put(2476800, 6.716736795699717);
      put(2488320, 9.358525543143344);
      put(2499840, 3.6810342886943204);
      put(2511360, -8.325091865690553);
      put(2522880, 7.206876778490084);
      put(2534400, 3.3889927926153973);
      put(2545920, -0.3518341100094631);
      put(2557440, -6.602095522740171);
      put(2568960, 5.81486433693118);
      put(2580480, 14.373569686472962);
      put(2592000, 1.316287768638631);
      put(2603520, -2.0866059126578307);
      put(2615040, -9.863175659904869);
      put(2626560, -7.081085214612699);
      put(2638080, -2.74645885036756);
      put(2649600, -4.754104313283733);
      put(2661120, -0.8707428555364913);
      put(2672640, 9.186359248324976);
      put(2684160, 7.8416000498015705);
      put(2695680, 0.9300601220571707);
      put(2707200, -4.365928760309771);
      put(2718720, 4.135487569588179);
      put(2730240, 3.279938679202246);
      put(2741760, 10.088086177698415);
      put(2753280, -3.9368905086458312);
      put(2764800, 7.659132549969854);
      put(2776320, 2.873456654916424);
      put(2787840, -1.4724266262796277);
      put(2799360, 3.992498546823583);
      put(2810880, -1.7838198990075966);
      put(2822400, 3.5331624747691706);
      put(2833920, 15.041885780550274);
      put(2845440, 1.9816731876182416);
      put(2856960, -1.7553940558311183);
      put(2868480, -17.113907293552757);
      put(2880000, -7.105853894817585);
      put(2891520, 11.11120763635597);
      put(2903040, -7.8393166802224385);
      put(2914560, -6.271885267417247);
      put(2926080, -1.9379256131235536);
      put(2937600, 2.480859416590249);
      put(2949120, -3.9098487197635734);
      put(2960640, -14.951660139051029);
      put(2972160, -7.524154725722326);
      put(2983680, 0.313311833272337);
      put(2995200, 3.305648501460638);
      put(3006720, -9.636162017753861);
      put(3018240, -7.841972575676152);
      put(3029760, 7.909291318606563);
      put(3041280, -1.0309649714725708);
      put(3052800, 4.038618931513895);
      put(3064320, 3.5811809103028054);
      put(3075840, 6.226924821115574);
      put(3087360, -2.588549389997411);
      put(3098880, -0.7376101390658505);
      put(3110400, -5.438620089620713);
      put(3121920, -3.7259907160117223);
      put(3133440, -5.039267469235256);
      put(3144960, 7.149530341232747);
      put(3156480, 4.043310541527319);
      put(3168000, 5.610980703536029);
      put(3179520, -4.303664782849538);
      put(3191040, -1.457209354663405);
      put(3202560, 0.9137181540305619);
      put(3214080, 4.48090743591024);
      put(3225600, 5.589526957165191);
      put(3237120, 0.6214188560750245);
      put(3248640, -1.244841567541016);
      put(3260160, -3.6291580518671083);
      put(3271680, 9.908990520733937);
      put(3283200, -7.66215030095276);
      put(3294720, 12.516143967514893);
      put(3306240, -9.636184003045344);
      put(3317760, 10.565581520166234);
      put(3329280, 4.05494788595675);
      put(3340800, -1.112339405361638);
      put(3352320, -3.4990596139571153);
      put(3363840, -1.2179616508538516);
      put(3375360, 3.0887646674953757);
      put(3386880, 1.5958953783476773);
      put(3398400, -3.138215492888321);
      put(3409920, 8.176017820795892);
      put(3421440, 1.401964914476423);
      put(3432960, 2.471066615203938);
      put(3444480, 1.2981736181148449);
    }};

    FilterHP filter = new FilterHP();
    double[] data = getFirstTimeSeries("src/test/resources/rotation/unrot_10_BHZ.512.seed")
        .getData();

    filter.init2(40);

    double[] result = filter.getFilterFunction().apply(data);

    assertEquals(3456000, result.length);
    for (Integer key : expected.keySet()) {
      assertTrue("Result more than 5E-5% different", (1 - expected.get(key) / result[key]) < 5E-07
      );
    }

  }
}