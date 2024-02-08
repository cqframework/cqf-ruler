package org.opencds.cqf.ruler.cdshooks.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.UrlUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ModuleConfigurationResolver {
	private static final String PATIENT = "Patient/{{context.patientId}}";
	private static final String ACTIVE_MEDICATION_ORDERS = "MedicationRequest?subject={{context.patientId}}&status=active&category=community&intent=order&_include=MedicationRequest:medication";
	private static final String ACTIVE_CATEGORIZED_CONDITIONS = "Condition?patient={{context.patientId}}&category=encounter-diagnosis,health-concern,problem-list-item&clinical-status=active";
	private static final String ENCOUNTERS_IN_PAST_YEAR = "Encounter?patient={{context.patientId}}&date=ge{{today}}";
	private static final String ACTIVE_OR_COMPLETED_SERVICE_REQUESTS = "ServiceRequest?patient={{context.patientId}}&status=active,completed";
	private static final String UDS_LABS_POST = "Observation?subject={{context.patientId}}&category=laboratory&date=ge{{today}}";
	//&code=58397-1,19660-0,19661-8,52951-1,14310-7,19659-2,8238-8,8237-0,14311-5,18392-1,3936-2,72825-3,16254-5,3937-0,19559-4,19560-2,59928-2,19558-6,18389-7,3786-1,16244-6,3787-9,42252-7,13479-1,19357-3,19358-1,19065-2,43985-1,43984-4,14314-9,8193-5,8192-7,14315-6,3393-6,70146-6,16226-3,3394-4,5935-2,5939-4,3970-1,3939-6,3940-4,3941-2,72792-5,19579-2,19580-0,19577-6,19578-4,3808-3,20548-4,3809-1,3810-9,72791-7,99110-9,61058-4,72790-9,33507-5,58358-3,19556-0,19557-8,55587-0,19554-5,21386-8,19555-2,3779-6,16235-4,3780-4,3656-6,3657-4,3658-2,3633-5,3634-3,3635-0,3627-7,3628-5,3629-3,3488-4,3489-2,3350-6,3351-4,3352-2,12331-5,69798-7,19653-5,19654-3,19651-9,19652-7,3901-6,20554-2,3902-4,3903-2,79233-3,79232-5,72794-1,79242-4,73686-8,72793-3,44424-0,19571-9,19572-7,19568-5,19569-3,14267-9,18358-2,19570-1,64138-1,19567-7,20546-8,19565-1,19566-9,42253-5,20545-0,18355-8,27409-2,64127-4,77772-2,59844-1,61048-5,45143-5,27085-0,73971-4,91037-2,79241-6,79246-5,72796-6,79237-4,67838-3,72795-8,87762-1,79236-6,79144-2,78858-8,15404-7,19514-9,19515-6,19510-7,19511-5,19512-3,12478-4,79244-0,79243-2,19460-5,19461-3,19456-3,16212-3,19458-9,19459-7,19454-8,19455-5,19452-2,19453-0,3564-2,20526-0,3565-9,3566-7,13498-1,31026-8,19427-4,19428-2,19423-3,19424-1,58925-9,19425-8,12477-6,19426-6,16367-5,58357-5,19347-4,19348-2,52957-8,19343-3,19344-1,16234-7,19346-6,13497-3,31025-0,19421-7,19422-5,19419-1,19420-9,20525-2,15366-8,79141-8,78817-4,79235-8,79234-1,64233-0,59135-4,59287-3,76492-8,40419-4,33280-9,56120-9,41464-9,19262-5,19263-3,19268-2,19266-6,19267-4,19059-5,19269-0,43983-6,14308-1,19261-7,8152-1,8151-3,14309-9,16369-1,3349-8,70138-3,20410-7,8150-5,19265-8,59329-3,59328-5,19690-7,19691-5,19686-5,19687-3,17504-2,19688-1,19689-9,6930-2,19684-0,19685-7,19682-4,19683-2,18414-3,20558-3,18415-0,73999-5,74000-1,74001-9,73998-7,19544-6,19545-3,19542-0,19543-8,12296-0,20543-5,9726-1,19530-5,19531-3,19528-9,16214-9,3732-5,33350-0,20542-7,3733-3,5679-6,78757-2,45185-6,12292-9,3568-3,12295-2,5608-5,16492-1,86192-2,98417-9,96555-8,86191-4,86195-5,86194-8,86607-9,19362-3,19363-1,52953-7,19359-9,19360-7,3397-7,20519-5,19361-5,3398-5,5937-8,16448-3,53743-1,80144-9,97154-9,50592-5,50594-1,86606-1,19409-2,19410-0,19405-0,19406-8,16632-2,19408-4,82723-8,42241-0,47400-7,26747-6,19287-2,19288-0,19293-0,19291-4,19292-2,19073-6,19294-8,52954-5,70145-8,70144-1,18282-4,19289-8,3427-2,70143-3,20413-1,42860-7,26760-9,19290-6,75651-0,78814-1,78813-3,13478-3,19417-5,19704-6,19418-3,19705-3,14312-3,21556-6,21557-4,19415-9,8175-2,8174-5,17872-3,19416-7,3426-4,14313-1,20524-5,3530-3,8173-7,19702-0,19703-8,3529-5,101249-1,78754-9,80113-4,19055-3,19383-9,19384-7,19381-3,19382-1,3435-5,20521-1,3436-3,72472-4,72471-6,78881-0,72468-2,78880-2,78879-4,72469-0,78878-6,87486-7,101182-4,101183-2,101181-6,101191-5,101192-3,101190-7,90747-7,101185-7,101186-5,101184-0,72470-8,72473-2,78850-5,72466-6,72803-0,72467-4,72802-2,73917-7,78849-7,78848-9,72805-5,78847-1,72804-8,78846-3,78845-5,78844-8,72462-5,78843-0,78842-2,72474-0,78841-4,78840-6,77771-4,72807-1,72806-3,72874-1,72875-8,72779-2,78839-8,72465-8,72778-4,78838-0,72464-1,73918-5,72460-9,72781-8,72780-0,72783-4,78837-2,72461-7,72782-6,72809-7,72808-9,72777-6,72776-8,73914-4,72459-1,88023-7,87487-5,87484-2,87488-3,87489-1,87490-9,87485-9,87491-7,87492-5,87493-3,87494-1,87495-8,72818-8,78783-8,72463-3,72817-0,88022-9,90746-9,67126-3,101188-1,101189-9,101187-3,86196-3,16223-0,12370-3,19696-4,19697-2,72928-5,19692-3,16194-3,4029-5,16238-8,19695-6,64133-2,19668-3,19669-1,39487-4,19666-7,16192-7,3949-5,16241-2,3950-3,64132-4,19657-6,19658-4,19655-0,16193-5,3925-5,16240-4,3926-3,3920-6,3921-4,3922-2,19540-4,19541-2,19536-2,19537-0,12315-8,17250-2,19539-6,64139-9,19375-5,19376-3,32056-4,19373-0,18385-5,3421-5,16237-0,11071-8,19371-4,19372-2,19368-0,16191-9,3419-9,16236-2,19370-6,19355-7,19356-5,19353-2,19354-0,12360-4,20518-7,3371-2,64128-2,19341-7,19342-5,59865-6,19339-1,16190-1,3339-9,16239-6,11230-0,53735-7,27165-0,41468-0,19271-6,19272-4,19277-3,19275-7,19276-5,19278-1,52956-0,70139-1,70155-7,19270-8,16429-3,3377-9,20664-9,20411-5,9426-8,16430-1,19274-0,3374-6,3375-3,3372-0,3373-8,19351-6,19352-4,19349-0,19350-8,12313-3,20517-9,14183-8,59322-8,59321-0,86224-3,78781-2,94116-1,16203-2,9351-8,59615-5,3313-4,19491-0,19492-8,19489-4,19490-2,20500-5,20531-0,11238-3,58363-3,19329-2,19330-0,19325-0,19326-8,61036-0,16348-5,19328-4,94115-3,58371-6,58426-8,21142-5,58427-6,10979-3,79256-4,58370-8,75228-7,74672-7,19716-0,19717-8,19714-5,16224-8,14192-9,16232-1,4070-9,19680-8,19681-6,19678-2,19679-0,17479-7,12311-7,19622-0,19623-8,14850-2,19620-4,19621-2,20552-6,3859-6,19476-1,19477-9,19474-6,19475-3,3654-1,16231-3,3655-8,94111-2,61033-7,51777-1,27084-3,64129-0,19618-8,19619-6,19614-7,19615-4,61040-2,16230-5,19617-0,19495-1,19496-9,19493-6,19494-4,20532-8,12602-9,94110-4,97159-8,64130-8,19444-9,19445-6,19443-1,18388-9,3550-1,16227-1,3551-9,21241-5,12314-1,86225-0,19403-5,19404-3,50843-2,19399-5,16204-0,19245-0,16229-7,19402-7,58364-1,94112-0,58365-8,61030-3,51776-3,28073-5,66128-0,19387-0,19388-8,59879-7,19385-4,19386-2,3458-7,20522-9,3459-5,33301-3,27036-3,19366-4,19367-2,59872-2,19364-9,19365-6,20520-3,3406-6,53736-5,58369-0,19700-4,19701-2,19698-0,16206-5,61060-0,20559-1,12382-8,94106-2,41467-2,19279-9,19280-7,19285-6,19283-1,19284-9,19064-5,19286-4,52955-2,70142-5,70141-7,14316-4,16195-0,3390-2,70140-9,20412-3,9428-4,19282-3,3387-8,3388-6,3385-2,3386-0,42235-2,99276-8,59908-4,12324-0,11240-9,11239-1,60044-5,59888-8,94114-6,52959-4,59635-3,12374-5,59938-1,94113-8,97160-6,58367-4,19626-1,19627-9,70213-4,19624-6,16202-4,3861-2,16228-9,15372-6,16744-5,58366-6,19522-2,19523-0,47115-1,19520-6,16205-7,18187-5,17088-6,3725-9,94109-6,19526-3,19527-1,19524-8,19525-5,20541-9,3726-7,3740-8,58372-4,79258-0,47120-1,58373-2,3754-9,58374-0,3755-6,86231-8,19589-1,19590-9,19585-9,19586-7,16233-9,19588-3,78758-0,58368-2,19640-2,19641-0,70214-2,19638-6,19639-4,12361-2,16201-6,3887-7,94107-0,44305-1,59877-1,17739-4,17738-6,4072-5,9503-4,60471-0,98982-2,89043-4,89042-6,97649-8,33047-2,65750-2,24349-3,72819-6,39456-9,59384-8,3299-5,13622-6,16282-6,12368-7,33277-5,89965-8,12321-6,19708-7,19709-5,47132-6,19706-1,19707-9,12319-0,20560-9,4053-5,61103-8,3510-5,19672-5,19673-3,19670-9,16221-4,3953-7,20556-7,3954-5,17441-7,59954-8,32105-9,3928-9,19548-7,19549-5,59925-8,19546-1,19547-9,12367-9,20544-3,17256-9,13906-3,3729-1,89972-4,60100-5,87777-9,89971-6,21047-6,19480-3,19481-1,19478-7,19479-5,3670-7,20530-2,3671-5,90466-4,90465-6,59904-3,14070-7,3651-7,89974-0,89973-2,3649-1,3624-4,3625-1,3626-9,77788-8,50844-0,32065-5,77961-1,50890-3,59171-9,89975-7,3497-5,3477-7,19397-9,19398-7,59884-7,19395-3,19396-1,3472-8,20523-7,3473-6,89977-3,89976-5,60213-6,87780-3,77878-7,89964-1,77877-9,89987-2,89986-4,12322-4,12320-8,29403-3,77881-1,32129-9,77880-3,77759-9,90471-4,92654-3,60116-1,77883-7,59963-9,59174-3,78877-8,59175-0,59958-9,74819-4,3987-5,89969-0,89968-2,77887-8,50891-1,40390-7,77888-6,87778-7,89970-8,42523-1,87788-6,59312-9,59172-7,98961-6,78884-4,78834-9,78833-1,97648-0,98962-4,98963-2,98964-0,98965-7,97647-2,87781-1,77760-7,89978-1,77761-5,78763-0,78871-1,78872-9,78870-3,90468-0,90467-2,77882-9,98960-8,77884-5,75242-8,16395-6,80131-6,78764-8,77758-1,78918-0,77757-3,47134-2,4084-0,12443-8,58015-9,12444-6,47127-6,4001-4,26978-7,61417-2,78835-6,59912-6,61418-0,3691-3,61419-8,3692-1,61411-5,78821-6,14692-8,61412-3,3533-7,61413-1,3534-5,14793-4,78819-0,14653-0,45144-3,3492-6,59940-7,58013-4,18470-5,91606-4,19316-9,58403-7,19313-6,19314-4,19319-3,19317-7,19318-5,19320-1,78889-3,80149-8,19312-8,6799-1,19315-1,11004-9,20663-1,16181-0,50543-8,61407-3,19333-4,19334-2,50804-4,19331-8,16225-5,3334-0,20515-3,16114-1,61426-3,77886-0,45301-9,93496-8,61427-1,3874-5,61428-9,3875-2,19337-5,19338-3,19335-9,19336-7,14596-1,20516-1,10978-5,98415-3,78882-8,92652-7,32112-5,92638-6,74662-8,75233-7,74655-2,3984-2,3985-9,3986-7,3973-5,3974-3,3975-0,47118-5,3737-4,3738-2,72813-9,19472-0,19473-8,59902-7,19470-4,19471-2,3645-9,20529-4,12439-6,72812-1,19630-3,19631-1,19628-7,19629-5,20553-4,12445-3,23865-9,61414-9,78822-4,14706-6,61415-6,3580-8,61416-4,3581-6,19247-6,59942-3,58014-2,12386-9,59867-2,3342-3,3343-1,75226-1,18360-8,89989-8,75235-2,89988-0,72775-0,78886-9,59977-9,32120-8,72774-3,18467-1,72773-5,78869-5,75234-5,72772-7,53882-7,59338-4,59336-8,59337-6,72785-9,78875-2,59949-8,92650-1,27109-8,72784-2,39591-3,89967-4,89966-6,75231-1,17320-3,75230-3,92640-2,78855-4,58030-8,4062-6,74387-2,78860-4,78859-6,74812-9,49690-1,2636-9,73692-6,72815-4,78824-0,78823-2,75229-5,72814-7,89991-4,77765-6,78818-2,59886-2,92645-1,59170-1,75232-9,73584-5,32136-4,78812-5,59169-3,60064-3,78832-3,89992-2,78831-5,60086-6,34331-9,25463-1,3721-8,18477-0,34330-1,59906-8,32074-7,89990-6,90470-6,90469-8,32095-2,34181-8,5694-5,5695-2,58356-7,77768-0,35664-2,22745-4,42242-8,34180-0,5644-0,46983-3,5645-7,58376-5,79239-0,77769-8,58375-7,58377-3,55349-5,58378-1,45324-1,16781-7,9357-5,59951-4,16219-8,3917-2,3918-0,3919-8,76659-2,98966-5,53746-4,94117-9,90890-5,53745-6,53747-2,51782-1,69739-1,87428-9,55419-6,72478-1,98416-1,78885-1,59975-3,92653-5,4065-9,92637-8,27059-5,16281-8,11000-7,3989-1,19583-4,19584-2,19581-8,16217-2,3813-3,20549-2,3814-1,18242-8,47111-0,3667-3,3668-1,3611-1,3612-9,59672-6,3598-0,72771-9,33340-1,94104-7,53787-8,33338-5,72770-1,33339-3,72769-3,94103-9,72768-5,59561-1,20536-9,20535-1,19497-7,20533-6,20501-3,20534-4,12432-1,86605-3,94105-4,49876-6,60677-2,12299-4,33041-5,21048-4,18391-3,59981-1,59176-8,22701-7,4025-3,4026-1,58404-5,19712-9,19713-7,19710-3,17718-8,43219-5,20561-7,17719-6,87760-5,86454-6,92904-2,18338-4,64134-0,19676-6,19677-4,19674-1,16222-2,3957-8,20557-5,3958-6,3959-4,12291-1,26786-4,19664-2,19665-9,19662-6,16220-6,3944-6,20555-9,3945-3,3946-1,3741-6,3742-4,3743-2,55350-3,3631-9,16210-7,3345-6,3346-4,3347-2,3469-4,3470-2,16610-8,4075-8,4076-6,4077-4,78767-1,58395-5,19644-4,19645-1,78873-7,70215-9,19642-8,19643-6,10998-3,16249-5,11246-6,86609-5,89302-4,90894-7,61423-0,77779-7,89303-2,61424-8,61425-5,95135-0,60276-3,58430-0,61197-0,46973-4,51954-6,3851-3,19607-1,19608-9,19605-5,16218-0,3839-8,20551-8,3840-6,3841-4,78768-9,58392-2,19599-0,19600-6,78861-2,70210-0,19597-4,13648-1,16196-8,3830-7,16251-1,3831-5,3832-3,74818-6,74817-8,101223-6,49831-1,19603-0,19604-8,19601-4,19602-2,3828-1,20550-0,3829-9,78770-5,41466-4,19552-9,19553-7,78857-0,52958-6,70149-0,70148-2,19550-3,16199-2,3773-9,70147-4,16246-1,3774-7,3775-4,58390-6,77752-4,77754-0,93495-0,58428-4,41858-2,58429-2,50542-0,42251-9,33527-3,58386-4,19534-7,19535-4,19532-1,16207-3,3746-5,16253-7,3747-3,3748-1,58387-2,77777-1,95798-5,58388-0,27920-8,58389-8,3869-5,58385-6,72384-1,19518-0,19519-8,19516-4,16213-1,3711-9,20540-1,3712-7,3713-5,58394-8,19487-8,19488-6,19486-0,18473-9,9834-3,16998-7,9835-0,89309-9,46971-8,51737-5,78769-7,58393-0,19484-5,19485-2,78830-7,19482-9,19483-7,12308-3,16252-9,3681-4,61420-6,78868-7,89304-0,61421-4,61422-2,51738-3,78766-3,58380-7,79260-6,79259-8,78828-1,59673-4,40839-3,11235-9,58381-5,3637-6,26867-2,11073-4,58382-3,77775-5,95797-7,43200-5,43199-9,58383-1,11075-9,93471-1,58379-9,66129-8,74810-3,74372-4,19464-7,19465-4,19462-1,19463-9,3618-6,20527-8,3619-4,16851-8,3573-3,3574-1,3575-8,19441-5,19442-3,19439-9,16198-4,3546-9,16755-1,3547-7,3540-2,3541-0,58391-4,19413-4,19414-2,70206-8,19411-8,13641-6,16197-6,3507-1,16250-3,3508-9,60514-7,86608-7,49829-5,89310-7,51739-1,58360-9,77787-0,77764-9,93494-3,16208-1,3414-0,49752-9,3415-7,16496-2,3416-5,58361-7,77774-8,82371-6,58362-5,49751-1,49753-7,89305-7,91027-3,58359-1,38373-7,3357-1,3359-7,3358-9,3309-2,3310-0,3311-8,86604-6,18383-0,16334-5,89300-8,75362-4,61429-7,79240-8,77879-5,72485-6,58401-1,58402-9,65807-0,89306-5,81754-4,65808-8,86610-3,18435-8,9396-3,100437-3,58398-9,19431-6,19432-4,59960-5,19429-0,16200-8,19141-1,16242-0,22065-7,3545-1,3544-4,19437-3,19438-1,19433-2,19434-0,19435-7,19436-5,16749-4,58399-7,19636-0,19637-8,19632-9,17376-5,3871-1,17377-3,19635-2,86197-1,86193-0,60126-0,41465-6,19296-3,19297-1,19301-1,19299-7,19300-3,19138-7,19302-9,52952-9,70151-6,21431-2,19295-5,8222-2,8221-4,18390-5,3879-4,5706-7,5707-5,70150-8,17384-9,8220-6,19298-9,72789-1,19612-1,19613-9,19609-7,18334-3,12309-1,19610-5,19611-3,78865-3,79377-8,82527-3,82524-0,72788-3,72787-5,78863-8,78862-0,78864-6,42618-9,92649-3,77207-9,89307-3,3842-2,75649-4,72730-5,96058-3,73995-3,96059-1,72732-1,72729-7,75643-7,72731-3,64131-6,19450-6,19451-4,19446-4,16211-5,14066-5,19448-0,19449-8,51955-3,51448-9,19379-7,19380-5,19377-1,19378-9,12333-1,16499-6,12395-0,12554-2,58384-9,19323-5,19595-8,19324-3,19596-6,70197-9,55520-1,19321-9,19322-7,10976-9,19593-3,20514-6,10975-1,19594-1,21050-0,19591-7,19592-5,14845-2,27073-6,89308-1,47004-7,51740-9,89301-6,78765-5,58396-3,19649-3,19650-1,78874-5,19646-9,18325-1,11247-4,17395-5,19648-5,46975-9,51736-7,93454-7,93453-9,93452-1,93451-3,93455-4,93456-2,93457-0,93458-8,93459-6,93460-4,93461-2,93462-0,93463-8,101161-8,101162-6,93464-6,93467-9,99079-6,93468-7,93469-5,99081-2,93470-3,93472-9,93473-7,93465-3,93466-1,101164-2,101165-9,101163-4,101167-5,101168-3,101166-7,101171-7,101180-8,101169-1,67822-7,94304-3,94305-0,93474-5
	private final IGenericClient prefetchClient;
	private final String patientId;
	private List<String> medicationIds;

	public ModuleConfigurationResolver(FhirContext fhirContext, Endpoint prefetchEndpoint, CdsHooksRequest request) {
		prefetchClient = fhirContext.newRestfulGenericClient(prefetchEndpoint.getAddress());
		if (prefetchEndpoint.getHeader() != null) {
			AdditionalRequestHeadersInterceptor headerInterceptor = new AdditionalRequestHeadersInterceptor();
			for (ModuleConfigurationResolver.HeaderInfo header : getHeaderNameValuePairs(prefetchEndpoint.getHeader())) {
				headerInterceptor.addHeaderValue(header.getName(), header.getValue());
			}
			prefetchClient.registerInterceptor(headerInterceptor);
		}
		patientId = ((CdsHooksRequest.OrderSign) request).context.patientId;

		var draftOrders = CdsHooksUtil.getDraftOrders(((CdsHooksRequest.OrderSign) request).context.draftOrders);
		medicationIds = draftOrders.stream().filter(MedicationRequest::hasMedicationReference)
			.map(order -> order.getMedicationReference().getReference()).collect(Collectors.toList());
	}

	public Bundle getPrefetchBundle() {
		var prefetchBundle = new Bundle();
		var urls = normalizeUrls();
		var executor = Executors.newFixedThreadPool(urls.size());
		var futureResources = urls.stream().map(m -> CompletableFuture.supplyAsync(() -> resourceFromUrl(m), executor)).collect(Collectors.toList());
		var resources = futureResources.stream().map(CompletableFuture::join).collect(Collectors.toList());

		for (var bundleResource : resources) {
			if (bundleResource instanceof Bundle) {
				((Bundle) bundleResource).getEntry().stream().filter(Bundle.BundleEntryComponent::hasResource).forEach(x -> prefetchBundle.addEntry().setResource(x.getResource()));
			} else {
				prefetchBundle.addEntry().setResource((Resource) bundleResource);
			}
		}

		return prefetchBundle;
	}

	public List<String> normalizeUrls() {
		String patientUrl = PATIENT.replace("{{context.patientId}}", patientId.replace("Patient/", ""));
		String activeMedsUrl = ACTIVE_MEDICATION_ORDERS.replace("{{context.patientId}}", patientId);
		String activeConditionsUrl = ACTIVE_CATEGORIZED_CONDITIONS.replace("{{context.patientId}}", patientId);
		String activeServiceReqsUrl = ACTIVE_OR_COMPLETED_SERVICE_REQUESTS.replace("{{context.patientId}}", patientId);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.YEAR, -1);
		String aYearAgo = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());

		String pastYearEncountersUrl = ENCOUNTERS_IN_PAST_YEAR.replace("{{today}}", aYearAgo).replace("{{context.patientId}}", patientId);
		String pastYearUdsLabsUrl = UDS_LABS_POST.replace("{{today}}", aYearAgo).replace("{{context.patientId}}", patientId);

		var ret = new ArrayList<>(Arrays.asList(patientUrl, activeMedsUrl, activeConditionsUrl, activeServiceReqsUrl, pastYearEncountersUrl, pastYearUdsLabsUrl));

		if (medicationIds != null && !medicationIds.isEmpty()) {
			ret.addAll(medicationIds);
		}

		return ret;
	}

	public IBaseResource resourceFromUrl(String theUrl) {
		UrlUtil.UrlParts parts = UrlUtil.parseUrl(theUrl);
		String resourceType = parts.getResourceType();
		if (StringUtils.isEmpty(resourceType)) {
			throw new InvalidRequestException(
				Msg.code(2383) + "Failed to resolve " + theUrl + ". Url does not start with a resource type.");
		}

		String resourceId = parts.getResourceId();
		String matchUrl = parts.getParams();
		if (resourceId != null) {
			return prefetchClient.read().resource(resourceType).withId(resourceId).execute();
		} else if (matchUrl != null) {
			var queryMap = UrlUtil.parseQueryString(matchUrl);
			Map<String, List<String>> whereMap = new HashMap<>();
			queryMap.forEach((x,y) -> whereMap.put(x, Arrays.asList(y)));
			return prefetchClient.search().forResource(resourceType).whereMap(whereMap).execute();
		} else {
			throw new InvalidRequestException(
				Msg.code(2384) + "Unable to translate url " + theUrl + " into a resource or a bundle.");
		}
	}

	public List<HeaderInfo> getHeaderNameValuePairs(List<StringType> headers) {
		List<HeaderInfo> headerNameValuePairs = new ArrayList<>();
		for (StringType header : headers) {
			// NOTE: assuming the headers will be key value pairs separated by a colon (key:
			// value)
			String[] headerNameAndValue = header.getValue().split("\\s*:\\s*");
			if (headerNameAndValue.length == 2) {
				headerNameValuePairs.add(new ModuleConfigurationResolver.HeaderInfo(headerNameAndValue[0], headerNameAndValue[1]));
			}
		}
		return headerNameValuePairs;
	}

	static class HeaderInfo {
		private final String name;
		private final String value;

		public HeaderInfo(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}
}
