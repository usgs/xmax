package com.isti.traceview.filters;

import static asl.utils.timeseries.TimeSeriesUtils.getFirstTimeSeries;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.Test;

public class LowPassTest {

  @Test
  public void basicFilterTest() throws Exception {

    //Expected results generated using scipy's butterworth Bandpass filter
    Map<Integer, Double> expected = new HashMap<Integer, Double>() {{
      put(0, -1.901968022707416e-07);
      put(11520, -248.48086236561448);
      put(23040, -249.45588679726305);
      put(34560, -260.0087956202488);
      put(46080, -252.72618993834712);
      put(57600, -268.44808083103914);
      put(69120, -242.3559841141287);
      put(80640, -260.887439713956);
      put(92160, -261.3803871363335);
      put(103680, -251.84507608188846);
      put(115200, -262.2123455769542);
      put(126720, -254.82447414941538);
      put(138240, -257.74830244605266);
      put(149760, -261.8398231864212);
      put(161280, -266.1516845192473);
      put(172800, -259.33865520440156);
      put(184320, -257.6547796425421);
      put(195840, -253.26157047489227);
      put(207360, -267.9197996591439);
      put(218880, -249.6382473290055);
      put(230400, -263.77123824146133);
      put(241920, -265.6395859544937);
      put(253440, -262.308090429145);
      put(264960, -259.4096032107182);
      put(276480, -267.6939140341391);
      put(288000, -262.53572122715184);
      put(299520, -258.2386328407075);
      put(311040, -275.9875912211441);
      put(322560, -274.57670429131986);
      put(334080, -277.2740447546974);
      put(345600, -279.4075945949913);
      put(357120, -277.6930212229101);
      put(368640, -283.5195771928348);
      put(380160, -282.40705025107997);
      put(391680, -290.55622670109335);
      put(403200, -294.86762324084157);
      put(414720, -293.25297899732266);
      put(426240, -304.50926452016176);
      put(437760, -313.76138447251867);
      put(449280, -304.54919271857375);
      put(460800, -302.0339942153666);
      put(472320, -312.6966045289133);
      put(483840, -312.67324196571445);
      put(495360, -319.57360351806557);
      put(506880, -318.1763761805484);
      put(518400, -323.0013823886287);
      put(529920, -331.7884153612659);
      put(541440, -343.3088253975115);
      put(552960, -326.3018718606343);
      put(564480, -344.470771150078);
      put(576000, -353.6973250734019);
      put(587520, -343.46699344574387);
      put(599040, -341.08045184509626);
      put(610560, -358.6201562447149);
      put(622080, -338.13451430915796);
      put(633600, -363.156777903603);
      put(645120, -358.6952012555642);
      put(656640, -359.297433925562);
      put(668160, -365.0889361304999);
      put(679680, -384.21244705428137);
      put(691200, -365.5497069001982);
      put(702720, -373.316807402938);
      put(714240, -374.94885207129266);
      put(725760, -371.61746173080513);
      put(737280, -384.2172728713518);
      put(748800, -373.86158776512997);
      put(760320, -384.72054723010115);
      put(771840, -383.6148664004067);
      put(783360, -392.72003837189465);
      put(794880, -394.325939244378);
      put(806400, -395.2616120260611);
      put(817920, -394.63252029003115);
      put(829440, -386.2511016299317);
      put(840960, -400.82432525379454);
      put(852480, -390.93651955278466);
      put(864000, -395.49705695321757);
      put(875520, -391.2040330372905);
      put(887040, -397.99804378426177);
      put(898560, -384.3878031212819);
      put(910080, -395.4573992211791);
      put(921600, -395.0863810681362);
      put(933120, -413.9296201584757);
      put(944640, -405.8259598692593);
      put(956160, -394.3617027337864);
      put(967680, -402.8916287737681);
      put(979200, -407.41225126060226);
      put(990720, -411.03810056965085);
      put(1002240, -405.8244750792167);
      put(1013760, -404.1265073412934);
      put(1025280, -399.7185702928848);
      put(1036800, -407.1157463800396);
      put(1048320, -410.5707012358537);
      put(1059840, -401.65857024058306);
      put(1071360, -390.72107464480524);
      put(1082880, -399.46987519104147);
      put(1094400, -401.1569344005207);
      put(1105920, -387.27159813656664);
      put(1117440, -400.81585994334984);
      put(1128960, -397.2540401921727);
      put(1140480, -397.0147515769886);
      put(1152000, -394.9362508888131);
      put(1163520, -380.07651159885023);
      put(1175040, -375.68995109255354);
      put(1186560, -394.54450537561326);
      put(1198080, -384.9738233564593);
      put(1209600, -375.06274195635405);
      put(1221120, -356.677673177852);
      put(1232640, -327.816357251553);
      put(1244160, -560.6017289997687);
      put(1255680, -354.8286723569821);
      put(1267200, -380.6018457778126);
      put(1278720, -375.5771642017991);
      put(1290240, -357.6465671372731);
      put(1301760, -362.10771827923605);
      put(1313280, -367.424048175977);
      put(1324800, -366.42880937384456);
      put(1336320, -363.680361231551);
      put(1347840, -359.7703900256267);
      put(1359360, -366.179522436666);
      put(1370880, -351.2729923509333);
      put(1382400, -358.59814778289854);
      put(1393920, -356.7660069604036);
      put(1405440, -349.4964195677092);
      put(1416960, -350.5462871285367);
      put(1428480, -344.49967599927237);
      put(1440000, -354.3116884736642);
      put(1451520, -334.84933176597747);
      put(1463040, -337.95130932663454);
      put(1474560, -338.71641842080874);
      put(1486080, -326.0612312449212);
      put(1497600, -320.74908565363);
      put(1509120, -323.7138118421824);
      put(1520640, -316.21530157185435);
      put(1532160, -315.3384225881946);
      put(1543680, -312.6381982300168);
      put(1555200, -326.57015141659986);
      put(1566720, -313.0449401056029);
      put(1578240, -316.2858722233046);
      put(1589760, -323.708892787343);
      put(1601280, -306.0637403116399);
      put(1612800, -313.3604406816443);
      put(1624320, -309.20251943884415);
      put(1635840, -302.04920783959363);
      put(1647360, -309.5718470946161);
      put(1658880, -300.98452959735647);
      put(1670400, -295.1748864288949);
      put(1681920, -295.6080112145406);
      put(1693440, -296.44334878341857);
      put(1704960, -285.3093187425122);
      put(1716480, -279.4004112823652);
      put(1728000, -294.5740482391255);
      put(1739520, -291.04009077158565);
      put(1751040, -283.86341005207566);
      put(1762560, -282.8476852070571);
      put(1774080, -286.17535579425993);
      put(1785600, -279.7453034061502);
      put(1797120, -281.92950408311134);
      put(1808640, -293.37663212535745);
      put(1820160, -285.44198700891667);
      put(1831680, -296.0683128841926);
      put(1843200, -282.7366477233242);
      put(1854720, -291.250439660917);
      put(1866240, -279.90408004420306);
      put(1877760, -287.35583438476465);
      put(1889280, -283.1058383097906);
      put(1900800, -282.88592660495385);
      put(1912320, -288.8610832635891);
      put(1923840, -282.2576783123324);
      put(1935360, -289.5535020259924);
      put(1946880, -283.43326521626045);
      put(1958400, -287.16681428874745);
      put(1969920, -297.9317339238527);
      put(1981440, -292.4091975396501);
      put(1992960, -292.22304057718725);
      put(2004480, -301.76293171135626);
      put(2016000, -309.01772184581193);
      put(2027520, -321.4364807535737);
      put(2039040, -340.123884367346);
      put(2050560, -331.5830855672939);
      put(2062080, -336.304402383423);
      put(2073600, -352.9362367004835);
      put(2085120, -363.631105447189);
      put(2096640, -357.0571690895291);
      put(2108160, -362.0686681913547);
      put(2119680, -353.787085045475);
      put(2131200, -356.3474622436702);
      put(2142720, -368.4975304095111);
      put(2154240, -368.7933860963183);
      put(2165760, -354.0456862757335);
      put(2177280, -369.1725755333888);
      put(2188800, -364.33077440758785);
      put(2200320, -368.04986335227073);
      put(2211840, -368.5723899329628);
      put(2223360, -365.8581408411385);
      put(2234880, -358.8901925756187);
      put(2246400, -376.8192831906141);
      put(2257920, -375.0858789436303);
      put(2269440, -353.7240702573181);
      put(2280960, -363.9511812197276);
      put(2292480, -361.81403990565894);
      put(2304000, -362.43410088387213);
      put(2315520, -336.6648746821011);
      put(2327040, -337.7140422059269);
      put(2338560, -329.7955487381469);
      put(2350080, -335.7362530065883);
      put(2361600, -321.3549710389127);
      put(2373120, -330.7942123898471);
      put(2384640, -328.21590801852443);
      put(2396160, -330.12936739976897);
      put(2407680, -322.3801166908879);
      put(2419200, -323.44897121973423);
      put(2430720, -342.81034136494713);
      put(2442240, -327.4612925912783);
      put(2453760, -314.0961996912479);
      put(2465280, -325.7377992321857);
      put(2476800, -332.76185272075077);
      put(2488320, -326.1454587831328);
      put(2499840, -320.175514747464);
      put(2511360, -334.99419744561624);
      put(2522880, -340.67500531354716);
      put(2534400, -339.13431170161624);
      put(2545920, -344.85862662456447);
      put(2557440, -340.10405488827996);
      put(2568960, -338.64674429338714);
      put(2580480, -342.0406689898547);
      put(2592000, -341.7360914732313);
      put(2603520, -343.8978438745135);
      put(2615040, -349.23092824721465);
      put(2626560, -356.12922769631797);
      put(2638080, -333.0710968448849);
      put(2649600, -336.5193667630232);
      put(2661120, -338.3889607995043);
      put(2672640, -347.8823427954395);
      put(2684160, -346.6351522757241);
      put(2695680, -351.5446690617444);
      put(2707200, -349.6161165676594);
      put(2718720, -344.5839942882841);
      put(2730240, -350.14814825681395);
      put(2741760, -351.6360443339446);
      put(2753280, -332.7437657250133);
      put(2764800, -347.93042901744155);
      put(2776320, -358.91469157777834);
      put(2787840, -351.33606526572623);
      put(2799360, -341.78495603539324);
      put(2810880, -363.39008146095284);
      put(2822400, -350.20972504195134);
      put(2833920, -364.13122677736965);
      put(2845440, -361.62671851667506);
      put(2856960, -362.3611976596311);
      put(2868480, -358.0409575192846);
      put(2880000, -358.07229138468915);
      put(2891520, -365.74908870868404);
      put(2903040, -353.5151647210262);
      put(2914560, -353.9893736277551);
      put(2926080, -343.62295610170975);
      put(2937600, -349.8940554778827);
      put(2949120, -341.30635750796745);
      put(2960640, -336.9561374523772);
      put(2972160, -335.15045181689896);
      put(2983680, -355.6976296489718);
      put(2995200, -332.9886146544416);
      put(3006720, -330.088283588572);
      put(3018240, -343.701395652301);
      put(3029760, -348.1723715766464);
      put(3041280, -339.37931776836837);
      put(3052800, -334.70015065293313);
      put(3064320, -329.0658856592933);
      put(3075840, -334.9701136682462);
      put(3087360, -334.35362908650353);
      put(3098880, -335.7359966681469);
      put(3110400, -325.3707289218242);
      put(3121920, -331.25173054110627);
      put(3133440, -324.45753264436166);
      put(3144960, -331.8513272510701);
      put(3156480, -315.15446035503953);
      put(3168000, -328.91120004250837);
      put(3179520, -322.83129183570003);
      put(3191040, -313.1236377404802);
      put(3202560, -326.59490152611903);
      put(3214080, -310.55570670559706);
      put(3225600, -314.0677337286481);
      put(3237120, -299.62795610275856);
      put(3248640, -321.76322795653334);
      put(3260160, -300.1962084722899);
      put(3271680, -276.16998574653746);
      put(3283200, -309.91928021090376);
      put(3294720, -319.05120900209465);
      put(3306240, -267.96725733683076);
      put(3317760, -252.533953220004);
      put(3329280, -299.6239203350933);
      put(3340800, -323.32391513749195);
      put(3352320, -301.9064883725224);
      put(3363840, -305.7618704149148);
      put(3375360, -316.51238976285003);
      put(3386880, -285.9184439151485);
      put(3398400, -288.03836386900286);
      put(3409920, -282.70308232062837);
      put(3421440, -328.81131156994104);
      put(3432960, -272.33211147620244);
      put(3444480, -274.0932209969156);
    }};

    Function<double[], double[]> filter = Filter.LOWPASS.getFilter(40);
    double[] data = getFirstTimeSeries("src/test/resources/rotation/unrot_10_BHZ.512.seed")
        .getData();

    double[] result = filter.apply(data);

    assertEquals(3456000, result.length);
    for (Integer key : expected.keySet()) {
      assertTrue("Result more than 8E-6% different", (1 - expected.get(key) / result[key]) < 8E-08);
    }

  }
}