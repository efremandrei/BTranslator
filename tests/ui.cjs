// Browser regression tests use an explicit Android bridge fixture. Real Android input
// interception / injection is verified separately by TranslatorIntegrationTest.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const assert=require('node:assert/strict'),fs=require('node:fs');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.CHROME_EXECUTABLE?{executablePath:process.env.CHROME_EXECUTABLE}:{})});
 const context=await browser.newContext({viewport:{width:412,height:900},hasTouch:true,isMobile:true,reducedMotion:'reduce'});
 await context.addInitScript(()=>{
  const saved=JSON.parse(localStorage.getItem('fixture')||'null');
  window.fixture=saved||{android:true,version:'1.0.0',build:1,connected:false,active:false,status:'Service off',lastInput:'No input received',lastGesture:'No gesture sent',samples:0,completed:0,cancelled:0,selectedName:'',devices:[{id:7,name:'Fixture <USB> pointer',descriptor:'fixture-mouse',source:'Mouse',external:true}],config:{threshold:35,idle:150,duration:220,span:60,lane:50,cooldown:450,reverse:false,vertical:false,scroll:true,device:'',light:false}};
  window.bridgeCalls=[];const persist=()=>localStorage.setItem('fixture',JSON.stringify(fixture));
  window.AndroidTranslator={state:()=>JSON.stringify(fixture),setTheme:light=>{fixture.config.light=light;persist();},configure:(key,value)=>{fixture.config[key]=['reverse','vertical','scroll'].includes(key)?!!value:value;persist();},selectDevice:descriptor=>{fixture.config.device=descriptor;fixture.selectedName=fixture.devices.find(d=>d.descriptor===descriptor).name;persist();},toggle:()=>{fixture.active=!fixture.active;persist();},detect:()=>bridgeCalls.push(['detect']),accessibility:()=>bridgeCalls.push(['accessibility']),bluetooth:()=>bridgeCalls.push(['bluetooth']),notifications:()=>bridgeCalls.push(['notifications']),repository:()=>bridgeCalls.push(['repository']),appInfo:()=>bridgeCalls.push(['appInfo']),testSwipe:(...args)=>bridgeCalls.push(['testSwipe',...args])};
 });
 const page=await context.newPage(),errors=[],external=[];
 page.on('pageerror',e=>errors.push(e.message));page.on('request',r=>{if(!r.url().startsWith('http://127.0.0.1:8766/'))external.push(r.url());});
 const out=process.env.QA_OUTPUT||'work/ui';fs.mkdirSync(out,{recursive:true});
 await page.goto('http://127.0.0.1:8766/');await page.getByRole('heading',{name:'Your remote. Real swipes.'}).waitFor();
 assert.ok(await page.locator('#start-stop').isDisabled());await page.screenshot({path:out+'/control-dark.png',fullPage:true});
 await page.locator('#choose-device').click();await page.locator('[data-device="0"]').click();await page.waitForTimeout(180);assert.equal(await page.locator('#device-name').innerText(),'Fixture <USB> pointer');assert.equal(await page.locator('#device-name img').count(),0);assert.ok(await page.locator('#start-stop').isDisabled());
 await page.evaluate(()=>{fixture.connected=true;refreshState();});assert.ok(await page.locator('#start-stop').isEnabled());await page.locator('#start-stop').click();await page.waitForTimeout(180);assert.match(await page.locator('#start-stop').innerText(),/Pause translation/);assert.match(await page.locator('#pointer-note').innerText(),/Other mice and touchpads are blocked/);
 await page.locator('#start-stop').click();await page.waitForTimeout(180);assert.match(await page.locator('#start-stop').innerText(),/Start translation/);
 await page.locator('[data-page=tune]').click();await page.locator('#threshold').fill('80');await page.locator('#threshold').dispatchEvent('input');await page.locator('#reverse').check();assert.equal(await page.locator('#out-threshold').innerText(),'80 dp');assert.match(await page.locator('#preview-copy').innerText(),/Directions reversed/);
 await page.reload();await page.locator('[data-page=tune]').click();assert.equal(await page.locator('#threshold').inputValue(),'80');assert.ok(await page.locator('#reverse').isChecked());await page.screenshot({path:out+'/tune.png',fullPage:true});
 await page.locator('[data-page=test]').click();await page.locator('#test-left').click();await page.waitForTimeout(100);const call=await page.evaluate(()=>bridgeCalls.find(x=>x[0]==='testSwipe'));assert.equal(call[1],'left');assert.ok(call[4]>100&&call[5]>100);assert.ok(call[2]>=0&&call[3]>=0);assert.match(await page.locator('#test-count').innerText(),/Swipe the card/,'Test button must not fake a completed touch');
 const box=await page.locator('#playground').boundingBox(),cdp=await context.newCDPSession(page);const y=box.y+box.height/2;
 await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:box.x+box.width*.8,y}]});
 for(let i=1;i<=6;i++)await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:box.x+box.width*(.8-i*.1),y}]});
 await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});assert.match(await page.locator('#test-count').innerText(),/1 touch swipe received.*left/);await page.screenshot({path:out+'/test.png',fullPage:true});
 await page.locator('#theme').click();await page.reload();assert.ok(await page.locator('body').evaluate(b=>b.classList.contains('light')));await page.screenshot({path:out+'/control-light.png',fullPage:true});
 await page.locator('#about').click();assert.match(await page.locator('#modal-body').innerText(),/Andrei Efremuahkin/);await page.locator('#reduce-motion').check();await page.locator('#close-modal').click();assert.ok(await page.locator('body').evaluate(b=>b.classList.contains('reduce-motion')));
 await page.locator('[data-page=help]').click();assert.equal(await page.locator('.faq').count(),11);await page.getByText('What happens to other mice?',{exact:true}).click();assert.match(await page.locator('.faq[open]').innerText(),/Finger touch is not intercepted/);
 for(const size of [{width:320,height:640},{width:412,height:900},{width:800,height:1000}]){await page.setViewportSize(size);for(const tab of ['control','tune','test','help']){await page.locator(`[data-page=${tab}]`).click();assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,`${tab} overflow at ${size.width}`);}}
 assert.deepEqual(errors,[]);assert.deepEqual(external,[]);console.log('PASS: setup gating, escaped input names, start/pause, config/theme persistence, actual browser touch handling, no fake test results, About/motion/help, 12 layouts, no external requests or script errors.');
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1);});
