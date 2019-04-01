package com.luxoft.blockchainlab.hyperledger.indy.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a particular attribute of a credential
 */
data class CredentialFieldReference(
    val fieldName: String,
    @JsonProperty("schema_id") override val schemaIdRaw: String,
    @JsonProperty("credential_definition_id") override val credentialDefinitionIdRaw: String
) : ContainsSchemaId, ContainsCredentialDefinitionId

/**
 * Represents predicate
 */
data class CredentialPredicate(val fieldReference: CredentialFieldReference, val value: Int, val type: String = ">=")


/**
 * Represents credential reference
 */
data class ReferentCredential(val key: String, val credentialUUID: String)

/**
 * Represents data which is needed for verifier to verify proof
 * Data in this data class is stored as JSON
 */
data class DataUsedInProofJson(
    val schemas: String,
    val credentialDefinitions: String,
    val revocationRegistryDefinitions: String,
    val revocationRegistries: String
)

/**
 * Represents proof request credentials
 *
 * Example:
 * {
 *  "attrs":{
 *      "attr0_referent":[
 *          {
 *              "cred_info":{
 *                  "referent":"6e74e8e2-6009-4293-aa98-fac05a94feb4",
 *                  "attrs":{
 *                      "name":"Alex",
 *                      "height":"175",
 *                      "age":"28",
 *                      "sex":"male"
 *                  },
 *                  "schema_id":"V4SGRU86Z58d6TV7PBUe6f:2:gvt:1.0",
 *                  "cred_def_id":"V4SGRU86Z58d6TV7PBUe6f:3:CL:11:TAG_1",
 *                  "rev_reg_id":null,
 *                  "cred_rev_id":null
 *              },
 *              "interval":null
 *          }
 *      ],
 *      "attr1_referent":[
 *          {
 *              "cred_info":{
 *                  "referent":"a744d31a-cd34-4cd2-80ab-8edfac57215c",
 *                  "attrs":{
 *                      "period":"8",
 *                      "status":"partial"
 *                  },
 *                  "schema_id":"V4SGRU86Z58d6TV7PBUe6f:2:xyz:1.0",
 *                  "cred_def_id":"V4SGRU86Z58d6TV7PBUe6f:3:CL:13:TAG_1",
 *                  "rev_reg_id":null,
 *                  "cred_rev_id":null
 *              },
 *              "interval":null
 *          }
 *      ]
 *  },
 *  "predicates":{
 *      "predicate0_referent":[
 *          {
 *              "cred_info":{
 *                  "referent":"6e74e8e2-6009-4293-aa98-fac05a94feb4",
 *                  "attrs":{
 *                      "name":"Alex",
 *                      "height":"175",
 *                      "age":"28",
 *                      "sex":"male"
 *                  },
 *                  "schema_id":"V4SGRU86Z58d6TV7PBUe6f:2:gvt:1.0",
 *                  "cred_def_id":"V4SGRU86Z58d6TV7PBUe6f:3:CL:11:TAG_1",
 *                  "rev_reg_id":null,
 *                  "cred_rev_id":null
 *              },
 *              "interval":null
 *          }
 *      ],
 *      "predicate1_referent":[
 *          {
 *              "cred_info":{
 *                  "referent":"a744d31a-cd34-4cd2-80ab-8edfac57215c",
 *                  "attrs":{
 *                      "period":"8",
 *                      "status":"partial"
 *                  },
 *                  "schema_id":"V4SGRU86Z58d6TV7PBUe6f:2:xyz:1.0",
 *                  "cred_def_id":"V4SGRU86Z58d6TV7PBUe6f:3:CL:13:TAG_1",
 *                  "rev_reg_id":null,
 *                  "cred_rev_id":null
 *              },
 *              "interval":null
 *          }
 *      ]
 *  }
 * }
 */
data class ProofRequestCredentials(
    @JsonProperty("attrs") val attributes: Map<String, List<CredentialReferenceInfo>>,
    val predicates: Map<String, List<CredentialReferenceInfo>>
)

/**
 * Reference to a credential with additional data that is used to create proof request
 *
 * @param credentialInfo        credential reference itself
 * @param interval              interval of non-revocation, can be null if revocation is disabled
 */
data class CredentialReferenceInfo(
    @JsonProperty("cred_info") val credentialInfo: CredentialReference,
    val interval: Interval? = null
)

data class CredentialReference(
    @JsonProperty("schema_id") override val schemaIdRaw: String,
    @JsonProperty("cred_def_id") override val credentialDefinitionIdRaw: String,
    val referent: String,
    @JsonProperty("attrs") val attributes: RawJsonMap,
    @JsonProperty("cred_rev_id") val credentialRevocationId: String?,
    @JsonProperty("rev_reg_id") override val revocationRegistryIdRaw: String?
) : ContainsSchemaId, ContainsCredentialDefinitionId, ContainsRevocationRegistryId

data class RequestedCredentials(
    val requestedAttributes: Map<String, RequestedAttributeInfo>,
    val requestedPredicates: Map<String, RequestedPredicateInfo>,
    val selfAttestedAttributes: Map<String, String> = hashMapOf()
)

data class RequestedAttributeInfo(
    @JsonProperty("cred_id") val credentialId: String,
    val revealed: Boolean = true,
    @JsonInclude(JsonInclude.Include.NON_NULL) val timestamp: Long?
)

data class RequestedPredicateInfo(
    @JsonProperty("cred_id") val credentialId: String,
    @JsonInclude(JsonInclude.Include.NON_NULL) val timestamp: Long?
)

/**
 * Represents proof request
 *
 * Format:
 *     {
 *         "name": string,
 *         "version": string,
 *         "nonce": string,
 *         "requested_attributes": { // set of requested attributes
 *              "<attr_referent>": <attr_info>, // see below
 *              ...,
 *         },
 *         "requested_predicates": { // set of requested predicates
 *              "<predicate_referent>": <predicate_info>, // see below
 *              ...,
 *          },
 *         "non_revoked": Optional<<non_revoc_interval>>, // see below,
 *                        // If specified prover must proof non-revocation
 *                        // for date in this interval for each attribute
 *                        // (can be overridden on attribute level)
 *     }
 *
 *     where
 *
 *
 *     attr_referent: Describes requested attribute
 *     {
 *         "name": string, // attribute name, (case insensitive and ignore spaces)
 *         "restrictions": Optional<[<attr_filter>]> // see below,
 *                          // if specified, credential must satisfy to one of the given restriction.
 *         "non_revoked": Optional<<non_revoc_interval>>, // see below,
 *                        // If specified prover must proof non-revocation
 *                        // for date in this interval this attribute
 *                        // (overrides proof level interval)
 *     }
 *     predicate_referent: Describes requested attribute predicate
 *     {
 *         "name": attribute name, (case insensitive and ignore spaces)
 *         "p_type": predicate type (Currently >= only)
 *         "p_value": predicate value
 *         "restrictions": Optional<[<attr_filter>]> // see below,
 *                         // if specified, credential must satisfy to one of the given restriction.
 *         "non_revoked": Optional<<non_revoc_interval>>, // see below,
 *                        // If specified prover must proof non-revocation
 *                        // for date in this interval this attribute
 *                        // (overrides proof level interval)
 *     }
 *     non_revoc_interval: Defines non-revocation interval
 *     {
 *         "from": Optional<int>, // timestamp of interval beginning
 *         "to": Optional<int>, // timestamp of interval ending
 *     }
 *     filter:
 *     {
 *         "schema_id": string, (Optional)
 *         "schema_issuer_did": string, (Optional)
 *         "schema_name": string, (Optional)
 *         "schema_version": string, (Optional)
 *         "issuer_did": string, (Optional)
 *         "cred_def_id": string, (Optional)
 *     }
 */
data class ProofRequest(
    val version: String,
    val name: String,
    val nonce: String,
    val requestedAttributes: Map<String, CredentialAttributeReference>,
    val requestedPredicates: Map<String, CredentialPredicateReference>,
    val nonRevoked: Interval? = null
)

data class CredentialAttributeReference(
    override val name: String,
    @JsonProperty("schema_id") override val schemaIdRaw: String
) : AbstractCredentialReference(name, schemaIdRaw)

data class CredentialPredicateReference(
    override val name: String,
    val p_type: String,
    val p_value: Int,
    @JsonProperty("schema_id") override val schemaIdRaw: String
) : AbstractCredentialReference(name, schemaIdRaw)

abstract class AbstractCredentialReference(
    open val name: String,
    @JsonProperty("schema_id") override val schemaIdRaw: String
) : ContainsSchemaId

/**
 * Represents proof
 *
 * Example:
 * {
 *  "proof":{
 *      "proofs":[
 *          {
 *              "primary_proof":{
 *                  "eq_proof":{
 *                      "revealed_attrs":{
 *                          "status":"51792877103171595686471452153480627530895"
 *                      },
 *                      "a_prime":"93437784553816354542173952068698037431597866756608549851440930308749330572288563130866006263290624670056390726834531412808474437322517811692367297760879047991199882610543005549621028481828015040538685324057188327174523116216557734971621076751576101755367679399206286403422267579779988505723869032683079715412759457872083768465603051779634164233859667634748086137209571346117385090712047144345818713026567927244815273273936122470013243381119204721204611590527446371842893412573871930087219448269657429655767428937567088688568492578080906548116284860117238419205164609200156206072241439187577134141984472524260374727752",
 *                      "e":"1537629560563993724668426185387143032055273926226772946421029787053861303414843635060242660171923584398823806086001121222205299331520906",
 *                      "v":"215733004396985010136352934525349842616476464362988126013243317041668615220741824465578493405830392994358665279654131697991652338575536282718414103571016169611745385028377355707316663675515424636005307602527685318066320392939095373981164727880207534534222982275405410254920538863717227976119407673414724164911889340114816889426071206060493046131805458862027115656126062741377751991631448036994724281570249945556763371754730695109137431982616051894869106666685506330362364449386958608254175613222500857103269315256324607709036035487316641666467375117696995550862511702233740493504621793168017615181900362217684352361017440494253041959124186452063734829606658679885510870436731462476318494940903099993992125716093433485023717798429320556334055231842295194845006831099457716063467125020110572702235723365289231145243111705468571839138198791620185085299757688104090452252820457457441865888855470719462740433723483347472822308",
 *                      "m":{
 *                          "period":"11104392078391482931535678453544870183229956347494196302332576706394419869818416398159927795147586775069696481246240924647578007457468747538537724438271412586890015720987627577985"
 *                      },
 *                      "m1":"11001504881865941802070489507803390696673762236555322260603357527292378775103055790669777267068341515147299706986131480569762932377407279420602101788743716767658956963392765670652",
 *                      "m2":"11664288858407254016915830228230594802856243634879303121575238912081984305685493271969522017315745764464074705351177820515417747648271212169536842437706673272981641645221055506721"
 *                  },
 *                  "ge_proofs":[
 *                      {
 *                          "u":{
 *                              "2":"4855543131592496659104325890675208273040374541688351011172303409617860672447847263925741536718555173469305656279320858649973767110567646689347092003859001411631303614037857617728",
 *                              "0":"763966279222447022381716401516356595944740552946462891024419068260058557303303824537666674147732374821438633274744115955392612303856162083294360440974905297028788806666483875189",
 *                              "3":"2418085707595357259839454517652962075989625041046873517613914931382801938201034075796371679173834825297105210784858299971533560624721661203265601359593622656083253566341406085308",
 *                              "1":"958600182775922787941879072104025262330988478935749710488316692931758719151764441638696411795127046946644392338171503970074195180466582650439581156674363003318862961022543826774"
 *                          },
 *                          "r":{
 *                              "0":"3275863761531606716394492082826960946657389282579426905848944129041864480007418168455958299658529844791152060387550756966131810579423680299904250731895932844856464711378554413740642773166114937277840129687639949581950264848154111617423890224969594048013412871127245283897271496995056561839511176794386283396923465812158171206750594350264320691675827527716737652755867936822803654079957788502532844809525984638419658957860945779501113410139027743957227453501974273408879287239361639097306957927516447498404980864875965963816313612667872002000433959941712991952374040794586188290671466878344988585376871600634761769569572721767262717269233791620848277379635718941668276045908360694341495691612035734474249872110490787572",
 *                              "3":"1632339893502494569226819407410428192381167043923496855070475176393546329407943266435433790508096920059697682824451008545841625563143330683702744629454756567674248242611467834350793219491055065318114185456034722855037810129125426769261399750817310575275891846168851298017146074216911030405386485188246896889396030583304646322707821223805372702965004958447038750375182327047377293785519232231106276056808395332397297377351146486530256936941609414415675347667980223287507290597897101040504708318626979432217545630884593274729901362102664263886118850667840687039283070932014817932750824512688162554302619389523962353591182937645198695003769901212689351205418128950233488595035778704955299644343766868089010612071594562898",
 *                              "DELTA":"670741183766313645216255374866483602306637822486043156756251006051718100180885564733662352921509475870576235600692178576597086706825108110989150826488986171877389812858113114585556467802646553046901886866993370901917155436112559674375257767505926194308883113815631042196105086918812281157384585741671554045648384344800208008848697233430440540330010306454252492760614516283370288056739774220444876870119510377402028921244646131108241228926999248069150466529606698270615588750388587194939723402515554602119270463575016638880737575846429331807738253339570023669502546049641820871570325724223337353721908469884332935852374562204613365534524634314511109007622876632004040559701385089260624147552657081414955233469313195435",
 *                              "2":"3076043346865791231262425588290474604770060147524186243737526556278661110944980511239895985642979340894541359988191398830433714144067319462162026991335755102198543140496370131480999234060644271692485092756764495476382157484151724967624509980829644636884217343028948871440948597465405550089422808109575024545119233352298805501310180322296757649970475379022413955080582105516997008720949137398452955941261788358858385592552587390626484404083550084301236836490404317320730559059009142921554246196949931657088237897538633826592734950493835901123041560175546285010387264538841800763896967464140765224195284129793381811586777347190767308180060533026360138764943904219476543315691468555509980870850907206894132265212296981490",
 *                              "1":"1686687731642328900987756232200297266354711166188506358367191468740681334929082009783529872590428097973441297671493039763972088042913473974104923013236683157726556957469737096884468153948965048059768191859196596285225197049695048794933540142457475879440908620219854675400041256101998761068480762740372730382986110046976567031412979483122689828724793497836216951921365574560539131279993631683618257461392727843128061067038840258924689324715062147583599967006735242853515942136837464368452488961953331134265775180492939761144988627901319701851789463352811846752576231151810145634935277099333308881410517085413685593655501938535183179815185008494596862530228320524494202615985029969370596170931714815272904111917023947909"
 *                          },
 *                          "mj":"11104392078391482931535678453544870183229956347494196302332576706394419869818416398159927795147586775069696481246240924647578007457468747538537724438271412586890015720987627577985",
 *                          "alpha":"25819921071765193785396247385773983961934456673767628429140447786916532175529024596476177308270281261732786766458178608488022464548323094085140867110665655551047845328251933124485774199128782766298151802122231397825083988027079597220873828096401448919289005034165295252018331463470937685118352396359739685373351032675466289331134221274122469802229502475487915532907787400911829980543044055716318460254823751561110404183056338923320403484983826270931555874946641008984636213227553534637618185873732012255391434418817313700725334990824186618600291837540106144181413167042297631214260505904738094562223661026367884892961004097996791218058188450603877970288514962431388936873235039751781342528854806261785248779955826789104182270572893449347880893560508353136042281418692637546062874635676758693963686755324307288320949830713397757296022220651",
 *                          "t":{
 *                              "1":"91924964899387911743313960934354249502102963374043116901864255766702771484547483815676742143826089854802151380158810941980680932625952114303564873711365386564176054589373518924752359147513958119801259063408044345734331199470357395720660053616869642355098427940787545288636190974406847769609755333285142228230892701494857503678286949963921193451423055410963717001078897548336799946044383075929221092555970808965756638711492269522615084865563693530920603420540402886516747965065264307961904142231773635184248441251369999486801086736566613129953295756066983357738814423379813571518220901054758639691082158057216115161045",
 *                              "2":"32912424685655327064251762508772937248964960650172281472558162996728864527874441446120097297176095645268738921273410208005848808358260309962093339185959001741265395329139214730801652508791699718065002959229534915098691146139823167103214321183733135511605759167286725717194429727537223961618027280534000862448501217890112400866714894443620657167756309448013772742403777009837976901609333947862659038942246577176586741322329370664378289112252632784371500551252230923258599885166462553757065924595153843633244122006843915519397651485360229571807717503014864537374897686505872082307843346402029943384710044210003778189337",
 *                              "3":"43926171016375760030744794754765745301779901514236450321535697172093362766748957206469465939686436035937961759880191961169485229981808523344588314063989798556282002053276707260509072929750497640066412014055211912807995445389875446972467459463622382740720581970764780055601355313210557015739782666573456146158363041699809250215795185471039790014007649895807242398761515656308510665987739718627178894139987314539926456914228545273624906041001310540314782438195981234272538351686012665331527002132252552538818491250283044144455255360395496958099451242965203414616966253843606973285728514315410023097799114482954507125349",
 *                              "0":"92557204728206802609462336814308989400133005757215915794188689044337795660045060416989088118903491713451771836219319798266689504240676894412532120562292937202508278902183863195962979510655308509948547019527388943976585013076419055232559628150031200488604148020837571935119844373152631311731595457397816926379720884966616100276264487889159704246030604197766885241640961687259345340700471738058786244398609449778724061878412631895264770752995378805633575776481266741325331567629460977453922359270863763073027485376448244979192306165130687546528252949613489064807445714011062407634509809552290680043282223802248217798370",
 *                              "DELTA":"38388822429927102079474142707075229418576369637183491317949857415864716644076277219845177375046519875888926013363215320519192188688283571089802048478812700235882491688163667946546081516418108267917862911637918566567827862339897290461986013484001623109594673043265560741076616606122024883191194730911683166975781930569821315465833646976185954323072294399016202588920880241238951827168875085360518456209225571264504926076425425034319835736869980469816330581948381413845153265597983104003573052422927142963715752904594505293272963339514987462486344495081900871524975749963280095520843914022132502244481767166325785709930"
 *                          },
 *                          "predicate":{
 *                              "attr_name":"period",
 *                              "p_type":"GE",
 *                              "value":5
 *                          }
 *                      }
 *                  ]
 *              },
 *              "non_revoc_proof":null
 *          },
 *          {
 *              "primary_proof":{
 *                  "eq_proof":{
 *                      "revealed_attrs":{
 *                          "name":"1139481716457488690172217916278103335"
 *                      },
 *                      "a_prime":"47328948634470351519143160532790546328864613571047294153397782359532067668821866343201764570113810100065314459736286934069206686900218660941126491895709441448577583650728689189694486530482217218135250685357325584889793739539413701803454059592734533609977625144200860785127199104057528753511831969596626572078077466822803019670740330426091721970522496113524956194126268815370496401281438924698508986374823532718696807503978962485469054207974991183640860833878892654778369636540273838603275187639632700801183049652579085194788329880933518429263160715098717961810073373854329030775565087703269127778921810617175757055699",
 *                      "e":"56895902393968433947112156013172865915604909954956050233162561111009404969378824544090400375422157645674337356379824768859227008355906020",
 *                      "v":"177955081870698596833664310113316549319283609739673257286484399710754626246897189546085022201015024124404652794937655463799971529065357045408274760705388327406959601375604481761532382073467237410626988982944246670821811468161047532133722805771915132789095610053941715999687358896185208743242339074243307920712648009492850495124627885544578670556070790258161390140912178345906840315568822933250677007251503874805931807391282280525145148469645355932628170297776151639415632460869476302076482163847539825023662496293670603365057354539705035818009546765244326853734946252758363137542852973900573001156036716904275207788173915111023802330130081014899281490551633867829141964233027799721434733125068562705167130275426534468704149598308441763695455589701541895754062040421001433978991218138629548727695266646130100167621445130346281727185615612135269366452068670853746112442322320313964612793568037954702649623410108987423083103",
 *                      "m":{
 *                          "age":"13583082689326186576561794555295140012094545240335897011332799786824319627425001048056722155899593965487778901668145148954341625262748387890248145470514395695954882002170136502297",
 *                          "height":"7328300303851680357536221775349204874185818606612213893756588074086798305232244550577247807742915567596276780815516103539699590800477814853929750195134658579599459216920016286645",
 *                          "sex":"3821945437611985051357585973505668119586586536245863843003873159221429022776863899794844739259444938072521243947168067209454927709799623212955983438620373583164705621901949414619"
 *                      },
 *                      "m1":"11001504881865941802070489507803390696673762236555322260603357527292378775103055790669777267068341515147299706986131480569762932377407279420602101788743716767658956963392765670652",
 *                      "m2":"10539582316790697212624348849742958316627168096682815128125415153273493531815905570059363092660158865766075408377239947414554551578441058657432468066956450391815771909619513799640"
 *                  },
 *                  "ge_proofs":[
 *                      {
 *                          "u":{
 *                              "3":"2664478266396465852128419225598922876183444430915813829552280818647914328139104049036921494706541578282179215057206673249792408175931420591197068567991759869635434385928682359072",
 *                              "2":"2874789567070769223753620432473126702021558049153645154353252924505748047882918919329374816794686473101080251950774734940614056892584139960670160405243174065159937705954593640069",
 *                              "1":"12920862262853722339787243595393265859195313402483169929578619837757560620439871501094932813448926440784866976803866938146726843601205794191506364707351837117203093513598629165321",
 *                              "0":"9646032665563800347992153789426257499576988389822444437056627735906953721515178972956158279180279813189791532041594842831782575624818321567578378971443466953419936595347412892291"
 *                          },
 *                          "r":{
 *                              "0":"3715244206129132120118032137038069538968811625753282266403421520614781363155003208020279723009403402247077845804965641634757427247454373788134604890788700545317953333020752083488584183826773470547891049533842465073165024914663419133045227735150817677628535218264446868185735064351502189363437839790373039004239325685379063896359800014871354623082906562271997282537387000879881887742374122661117665079070089360502622803744894964456516077246266862697648255350491795871331367191636090116642326122572630341529676618896324000770924188318312805938125379170967415905110482868093464389437774754917779819090357848599946813878686088873584772002943780961486059757919081915651321084905225801965757945623643278799930089864237092457",
 *                              "DELTA":"1867295636191498684352941011427971200783156537264545467606286253260895767755963246472583964696730737441933364147876202878517594152797588364613949232732938828861514475327205689440227201053192295852170117874670912745167158963030846238041501160243866811227408541276478383623263706461547486697008082652791318660412839934902185750268963457319739451924859791555335024490616502660373291390150800798581603638918140104243866939631707341789575179074128751357951179804969566191658375193904580347607899578699162934364370334803497415820266159074430683958049568752145060896960176710857983914920539717008990548296716013391716063509975277890379186013467097990817658161327955549162208720098662188632325078989820245887241362431001009563",
 *                              "1":"561287355023649168682884407196368891146176701430640387658545569671207268405304272711557529779361054133203936853012323774277332476567657174884905206759583344217452091009067961283518938852946786447559981041617454386663155615345668623094136271200389306594136570468033283469248609732605472620910861450791846808599896199177237787820850488716029705312094184632508621098511800682591462471911452173038482799058086315532630209559985658119956997145511000739190609775440974624780783187906157444431963508486310643743841203135526007700817850893309618587588807734338612949750073101920450696462788838514489613044716285683235719602383875039170269926825548666663043574500598404811847409411448213408277025610387466520080052721512813829",
 *                              "2":"3558042926714515295249029088793509248706282139554394356395304057341955223560777508928958543670632526997537734183534630223399281700063227226054186592240066023350532972950609967698109624317622603493809705058243304759417711738743463679737843253615595783243393394309194645157946918766225927908473271223963393071283106032564179708656266443299288222320937246035660180126981232404381595823810527389027723520452194002555612714586214264783096907198989558084857092342382568377616570607971159589262019466999139173408094462755862551625437100445783258248044636161541444582559767541028463936959732263464635542092630995538263366154589777213377663421694424212064651827381912182307643170555065265013623657977872436069561423076968904117",
 *                              "3":"1442856926220651010062615811531969373855283420608218543187130111379781111847626840814242039477507126800202156226700933979054250609971810682712228380089340531728832516293436083896403198821568604660877975073364455730709254415796098541396484091874683373366136300998512305503718401878767459899913064846465306725690219180944870827191300195929390390970847251664505707848766259575276118080404357424071031579606167695541338983652122062341264570193444568580651759738954043858145108840511367737712566555538082598514965848135620205398029930629847106716889196363334378758436168444443060144728419086233943898847132957892219703002025446128332845731743018306397766698711518306904972971288372993910150311898639656522886226370069309519"
 *                          },
 *                          "mj":"13583082689326186576561794555295140012094545240335897011332799786824319627425001048056722155899593965487778901668145148954341625262748387890248145470514395695954882002170136502297",
 *                          "alpha":"60571685209802228542926669729424779594750114653590206478378938201856877128259328368355011252928898712972341398926966517999913835629765378669820540391928523946387509361377009253289732581385122729980002156427402751923039444217054931880296810906281207207918278288989307606588041388971890607836179215683331778432093636210130620871696464021666375326281803470991831214668546289081164009044998768155886113370394030171421816113753069358937500743499771417499151344705844497920840767926489247757456905103634030566928192209384449398254897516766063965149570935645446739850781809753101765178344928392159879006500726701066507689164543105961285560982590916261740406393040634702250274627733467516352786603297672903722178204837083905570226559473966796265289619314283988640388300011068820022550235955045198975314437356668393310058442186477281795590982918049",
 *                          "t":{
 *                              "1":"3917932745629582127219250038708092265566887155007120856339494719674790722960328077288315719190728882778302662881872080549549133299291382317431792267502912822498550590527566034288460614402922347299520082604334314826340397639414815909477823018872644582901324348324465176314520334938593789127174678604322625151863305737999854985400514798866774397039869659236705857652789966857880878838938305102752060917628356017909971630690582737878108167818491460961743106041265152419715981316229063702380204742431313380344530099532150196268566452559844179911069002953545897855218003398815458056571383147125609598195630378847668019511",
 *                              "2":"64774945772666467213459907412540104569692252756792711021211619040201316580314420002525634540499616007070183109128122674165389775414769599071653935559628566103537718813202211112532314814633270205797083890633463580199419019982050914745038560091281491684901596642570564245090157531977771539523652082202892876358944002867965591015192774783276966679142538182973694309760284637269319011504944624261564022735819987431009705922400478238491511489488500731375996532055548136657118424735806667221194913019119523009657517427709940285379267572597470958106660142112547763345176847341060883413947404741542158648832100652652642885338",
 *                              "3":"64918782781195804280876465078139845841124913026452141812258258106320409744062319366734490141355878792954599091818537663272449906340744397935922918530114322038853677510020487981537838572415595043448838160433274446111737680915688949528363127456759146669076528694282722954522918621064119240750773420957726510052635454769601105597402425481085036651621992624549802418925971436151278472370943775780325408159831700067847827705561868408154468712956426621131370851418631130879257285345516838655421783996770016197322485936671258382727846178980008054275214229529684265005961139121671935502471732923579408573510107590680395265576",
 *                              "0":"62684584528254149664357520553406197277224596930694097695298226093110291160485656459125421763402902774376410122711239106332325075860007307781872525273933964172923287359352574142630642008993342032042438044326019753344134393977583288045963020678212298499724661087882829923292789374097726807314263982788634886044069785910411766188694255055756043543249799549067571158135281040874937717063942124584159287770991168238067214526265824266943116523994090773312861862422023744360211114381817491723997587333077778762774919918561347427271143655268358117339896698116440092026493541386592280477778803927446190120589641095841529589738",
 *                              "DELTA":"12301970271461860944051707948260966652880954378010849587994918909298902847818581277523649239091954958076545678956896821290586070308227852819596203947038479326416325808907391266741705539171827662008214207570999872216978014532111946257686725673625389936546250445276945595460059576253755821390960546266584145323454065614178929748246794894739186255841987066657776131774423937478231294085335811880017217512118928798920979912080788477578228659737826609479271562946743283578570622022953624062848475934655528456591837215841271382571491750403407899250934913888374563113144270506204899963905010677884916530469130922343386153953"
 *                          },
 *                          "predicate":{
 *                              "attr_name":"age",
 *                              "p_type":"GE",
 *                              "value":18
 *                          }
 *                      }
 *                  ]
 *              },
 *              "non_revoc_proof":null
 *          }
 *      ],
 *      "aggregated_proof":{
 *          "c_hash":"95837096850430218504093225408781786197920103703738504206700427633890377110833",
 *          "c_list":[
 *              [
 *                  2,228,43,121,227,174,99,28,40,23,108,39,223,134,55,22,90,241,181,30,135,47,167...
 *              ],
 *              [
 *                  2,221,49,189,43,133,130,48,147,243,128,172,223,60,178,213,130,6,153,102,36,249...
 *              ],
 *              ...
 *         ]
 *     }
 *  },
 *  "requested_proof":{
 *      "revealed_attrs":{
 *          "attr0_referent":{
 *              "sub_proof_index":1,
 *              "raw":"Alex",
 *              "encoded":"1139481716457488690172217916278103335"
 *          },
 *          "attr1_referent":{
 *              "sub_proof_index":0,
 *              "raw":"partial",
 *              "encoded":"51792877103171595686471452153480627530895"
 *          }
 *      },
 *      "self_attested_attrs":{},
 *      "unrevealed_attrs":{},
 *      "predicates":{
 *          "predicate0_referent":{
 *              "sub_proof_index":1
 *          },
 *          "predicate1_referent":{
 *              "sub_proof_index":0
 *          }
 *      }
 *  },
 *  "identifiers":[
 *      {
 *          "schema_id":"V4SGRU86Z58d6TV7PBUe6f:2:xyz:1.0",
 *          "cred_def_id":"V4SGRU86Z58d6TV7PBUe6f:3:CL:13:TAG_1",
 *          "rev_reg_id":null,
 *          "timestamp":null
 *      },
 *      {
 *          "schema_id":"V4SGRU86Z58d6TV7PBUe6f:2:gvt:1.0",
 *          "cred_def_id":"V4SGRU86Z58d6TV7PBUe6f:3:CL:11:TAG_1",
 *          "rev_reg_id":null,
 *          "timestamp":null
 *      }
 *  ]
 * }
 */
data class ParsedProof(
    val proof: Proof,
    val requestedProof: RequestedProof,
    val identifiers: List<ProofIdentifier>
)

data class ProofInfo(
    @JsonProperty("proof_data") val proofData: ParsedProof
) {
    @JsonIgnore
    fun isAttributeExists(value: String) = proofData.requestedProof.revealedAttrs.values.any { it.raw == value }

    @JsonIgnore
    fun getAttributeValue(attrName: String) = proofData.requestedProof.revealedAttrs[attrName]

    @JsonIgnore
    operator fun get(attrName: String) = getAttributeValue(attrName)
}

data class ProofIdentifier(
    @JsonProperty("schema_id") override val schemaIdRaw: String,
    @JsonProperty("cred_def_id") override val credentialDefinitionIdRaw: String,
    @JsonProperty("rev_reg_id") override val revocationRegistryIdRaw: String?,
    val timestamp: Long?
) : ContainsSchemaId, ContainsRevocationRegistryId, ContainsCredentialDefinitionId

data class Proof(val proofs: List<ProofDetails>, val aggregatedProof: Any)

data class RevealedAttributeReference(val subProofIndex: Int, val raw: String, val encoded: String)

data class RevealedPredicateReference(@JsonProperty("sub_proof_index") val subProofIndex: Int)

data class RequestedProof(
    val revealedAttrs: Map<String, RevealedAttributeReference>,
    val selfAttestedAttrs: Map<String, RevealedAttributeReference>, // not tested
    val unrevealedAttrs: Map<String, CredentialReference>, // not tested
    val predicates: Map<String, RevealedPredicateReference>
)

data class ProofDetails(val primaryProof: Any, @JsonProperty("non_revoc_proof") val nonRevokedProof: Any?)
